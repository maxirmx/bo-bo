import webswingProto from './directdraw.proto';
const ProtoBuf = require('protobufjs');
import Util from './webswing-util';

export default class WebswingDirectDraw {
	constructor() {		
		let c = {};
		let config = {
			logDebug: c.logDebug || false,
            		ieVersion: c.ieVersion || false,
			onErrorMessage : c.onErrorMessage || function(message) {
				console.log(message.stack);
       },
       dpr: c.dpr
		};
		let ctxId = Math.floor(Math.random() * 0x10000).toString(16);
		let proto = webswingProto!=null? ProtoBuf.loadProto(webswingProto,"directdraw.proto"):ProtoBuf.loadProtoFile("/directdraw.proto");
		let WebImageProto = proto.build("org.webswing.directdraw.proto.WebImageProto");
		let InstructionProto = proto.build("org.webswing.directdraw.proto.DrawInstructionProto.InstructionProto");
		let SegmentTypeProto = proto.build("org.webswing.directdraw.proto.PathProto.SegmentTypeProto");
		let ArcTypeProto = proto.build("org.webswing.directdraw.proto.ArcProto.ArcTypeProto");
		let CyclicMethodProto = proto.build("org.webswing.directdraw.proto.CyclicMethodProto");
		let StrokeJoinProto = proto.build("org.webswing.directdraw.proto.StrokeProto.StrokeJoinProto");
		let StrokeCapProto = proto.build("org.webswing.directdraw.proto.StrokeProto.StrokeCapProto");
		let StyleProto = proto.build("org.webswing.directdraw.proto.FontProto.StyleProto");
		let CompositeTypeProto = proto.build("org.webswing.directdraw.proto.CompositeProto.CompositeTypeProto");
		let constantPoolCache = c.constantPoolCache || {};
		let fontsArray = [];
		let canvasBuffer = [];
       		let xorLayer;

		function draw64(data, targetCanvas) {
			return drawWebImage(WebImageProto.decode64(data), targetCanvas);
		}

		function drawBin(data, targetCanvas) {
			return drawWebImage(WebImageProto.decode(data), targetCanvas);
		}

		function drawProto(data, targetCanvas) {
			return drawWebImage(data, targetCanvas);
		}

		function drawWebImage(image, targetCanvas) {
			config.dpr = Util.dpr() || 1
			return new Promise(function(resolve, reject) {
				try {
					drawWebImageInternal(image, targetCanvas, resolve, reject);
				} catch (e) {
					config.onErrorMessage(e);
					reject(e);
				}
			});
		}
		
        function drawWebImageInternal(image, targetCanvas, resolve, reject) {
            let newCanvas;
            let renderStart = new Date().getTime();
            if (targetCanvas != null) {
                newCanvas = targetCanvas;
            } else {
                newCanvas = document.createElement("canvas");
                newCanvas.classList.add("webswing-canvas");
                newCanvas.getContext("2d").scale(config.dpr, config.dpr);
            }
            if (newCanvas.width != image.width * config.dpr || newCanvas.height != image.height * config.dpr) {
                newCanvas.width = image.width * config.dpr;
                newCanvas.height = image.height * config.dpr;
            }

            let imageContext = {
                canvas: newCanvas,
                graphicsStates: {},
                currentStateId: null
            };

            let images = populateConstantsPool(image.constants);
            prepareImages(images)
                .then(function () {
                    return initializeFontFaces(image.fontFaces)
                })
                .then(function () {
                    let ctx = imageContext.canvas.getContext("2d");
                    if (image.instructions != null) {
                        ctx.save();
                        image.instructions.reduce(function (seq, instruction) {
                            return seq.then(function (resolved) {
                                return interpretInstruction(ctx, instruction, imageContext);
                            });
                        }, Promise.resolve()).then(function () {
                            ctx.restore();
                            logRenderTime(renderStart, image);
                            resolve(imageContext.canvas);
                        }, function (error) {
                            ctx.restore();
                            reject(error);
                            config.onErrorMessage(error);
                        });
                    }
                }, function (error) {
                    config.onErrorMessage(error);
                });
        }

		function populateConstantsPool(constants) {
			let images = [];
			constants.forEach(function(constant) {
				constantPoolCache[constant.id] = constant;
				if (constant.image != null) {
					images.push(constant.image);
				} else if (constant.texture != null) {
					images.push(constant.texture.image);
				} else if (constant.glyph != null && constant.glyph.data != null){
					images.push(constant.glyph);
				}
			});
			return images;
		}

		function interpretInstruction(ctx, instruction, imageContext) {
	            let ctxOriginal = ctx;
	            let args = resolveArgs(instruction.args, constantPoolCache);
	            let graphicsState = imageContext.graphicsStates[imageContext.currentStateId];
	            let xorMode = isXorMode(graphicsState);
	            if (xorMode) {
	                ctx = initXorModeCtx(graphicsState, ctxOriginal);
	            }
	            switch (instruction.inst) {
	                case InstructionProto.GRAPHICS_CREATE:
	                    iprtGraphicsCreate(ctxOriginal, instruction.args[0], args, imageContext);
	                    break;
	                case InstructionProto.GRAPHICS_SWITCH:
	                    iprtGraphicsSwitch(ctxOriginal, instruction.args[0], imageContext);
	                    break;
	                case InstructionProto.GRAPHICS_DISPOSE:
	                    iprtGraphicsDispose(instruction.args[0], imageContext);
	                    break;
	                case InstructionProto.DRAW:
	                    iprtDraw(ctx, args, graphicsState.transform);
	                    break;
	                case InstructionProto.FILL:
	                    iprtFill(ctx, args);
	                    break;
	                case InstructionProto.DRAW_IMAGE:
	                    iprtDrawImage(ctx, args);
	                    break;
	                case InstructionProto.DRAW_WEBIMAGE:
	                    return iprtDrawWebImage(ctx, args, instruction.webImage);
	                    break;
	                case InstructionProto.DRAW_STRING:
	                    iprtDrawString(ctx, args, graphicsState.fontTransform);
	                    break;
	                case InstructionProto.COPY_AREA:
	                    iprtCopyArea(ctx, args);
	                    break;
	                case InstructionProto.SET_STROKE:
	                    graphicsState.strokeArgs = args;
	                    iprtSetStroke(ctxOriginal, args);
	                    break;
	                case InstructionProto.SET_PAINT:
	                    graphicsState.paintArgs = args;
	                    iprtSetPaint(ctxOriginal, args);
	                    break;
	                case InstructionProto.SET_COMPOSITE:
	                    graphicsState.compositeArgs = args;
	                    iprtSetComposite(ctxOriginal, args);
	                    break;
	                case InstructionProto.SET_FONT:
	                    graphicsState.fontArgs = args;
	                    graphicsState.fontTransform = iprtSetFont(ctxOriginal, args);
	                    break;
	                case InstructionProto.TRANSFORM:
	                    graphicsState.transform = concatTransform(graphicsState.transform, iprtTransform(ctxOriginal, args));
	                    break;
	                case InstructionProto.DRAW_GLYPH_LIST:
	                    iprtDrawGlyphList(ctx, args);
	                    break;
	                default:
	                    console.log("instruction code: " + instruction.inst + " not recognized");
			}
            if (xorMode) {
                let bbox = ctx.popBoundingBox();

                let xorModeColor = parseColorSamples(graphicsState.compositeArgs[0].composite.color);
                applyXorModeComposition(ctxOriginal, ctx, xorModeColor, bbox);
            }
            return Promise.resolve();
        }

        function isXorMode(graphicsState) {
            return graphicsState != null && graphicsState.compositeArgs != null && graphicsState.compositeArgs[0].composite.type == CompositeTypeProto.XOR_MODE;
        }

        function initXorModeCtx(graphicsState, original) {
            if (xorLayer == null) {
                xorLayer = document.createElement("canvas");
                xorLayer.getContext("2d").scale(config.dpr, config.dpr);
            }
            xorLayer.width = original.canvas.width;
            xorLayer.height = original.canvas.height;
            let ctx = xorLayer.getContext("2d");
            wrapContext(ctx);
            setCtxState(graphicsState, ctx);
            return ctx;
        }

        function wrapContext(ctx) {
            if (!ctx.wrapped) {
                ctx.wrapped = true;
                ctx.boundingBox = null;
                ctx.pathBBox = null;
                //track bounding boxes of changed areas:
                let beginPathOriginal = ctx.beginPath;
                ctx.beginPath = function () {
                    this.pathBBox = {};
                    this.pathBBox.minX = this.pathBBox.minY = 99999999999;
                    this.pathBBox.maxX = this.pathBBox.maxY = -99999999999;
                    return beginPathOriginal.call(this);
                };
                let setTransformOriginal = ctx.setTransform;
                ctx.setTransform = function (m11, m12, m21, m22, dx, dy) {
                    this.transfomMatrix = [m11, m12, m21, m22, dx, dy];
                    return setTransformOriginal.call(this, m11, m12, m21, m22, dx, dy);
                };

                let transformOriginal = ctx.transform;
                ctx.transform = function (m11, m12, m21, m22, dx, dy) {
                    this.transfomMatrix = concatTransform(this.transfomMatrix, [m11, m12, m21, m22, dx, dy]);
                    return transformOriginal.call(this, m11, m12, m21, m22, dx, dy);
                };

                ctx.updateMinMax = function (x, y) {
                    let tp = transformPoint(x, y, this.transfomMatrix);
                    if (tp.x < this.pathBBox.minX) this.pathBBox.minX = tp.x;
                    if (tp.x > this.pathBBox.maxX) this.pathBBox.maxX = tp.x;
                    if (tp.y < this.pathBBox.minY) this.pathBBox.minY = tp.y;
                    if (tp.y > this.pathBBox.maxY) this.pathBBox.maxY = tp.y;
                };

                let fillTextOriginal = ctx.fillText;
                ctx.fillText = function (text, x, y, maxWidth) {
                    this.pathBBox = {};
                    this.pathBBox.minX = this.pathBBox.minY = 99999999999;
                    this.pathBBox.maxX = this.pathBBox.maxY = -99999999999;
                    let width = maxWidth || this.measureText(text).width;
                    let height = this.measureText("M").width * 2;//approximation
                    this.updateMinMax(x - 3, y - height * 0.7);
                    this.updateMinMax(x - 3, y + height * 0.3);
                    this.updateMinMax(x + width * 1.2, y - height * 0.7);
                    this.updateMinMax(x + width * 1.2, y + height * 0.3);
                    this.setBoundingBox();
                    if (maxWidth === undefined) { //IE can not handle maxWidth==undefined
                        return fillTextOriginal.call(this, text, x, y);
                    } else {
                        return fillTextOriginal.call(this, text, x, y, maxWidth);
                    }
                };

                let moveToOriginal = ctx.moveTo;
                ctx.moveTo = function (x, y) {
                    this.updateMinMax(x, y);
                    return moveToOriginal.call(this, x, y);
                };

                let lineToOriginal = ctx.lineTo
                ctx.lineTo = function (x, y) {
                    this.updateMinMax(x, y);
                    return lineToOriginal.call(this, x, y);
                };

                let quadraticCurveToOriginal = ctx.quadraticCurveTo
                ctx.quadraticCurveTo = function (cpx, cpy, x, y) {
                    this.updateMinMax(x, y);
                    this.updateMinMax(cpx, cpy);
                    return quadraticCurveToOriginal.call(this, cpx, cpy, x, y);
                };

                let bezierCurveToOriginal = ctx.bezierCurveTo
                ctx.bezierCurveTo = function (cp1x, cp1y, cp2x, cp2y, x, y) {
                    this.updateMinMax(x, y);
                    this.updateMinMax(cp1x, cp1y);
                    this.updateMinMax(cp2x, cp2y);
                    return bezierCurveToOriginal.call(this, cp1x, cp1y, cp2x, cp2y, x, y);
                };

                let rectOriginal = ctx.rect
                ctx.rect = function (x, y, w, h) {
                    this.updateMinMax(x, y);
                    this.updateMinMax(x, y + h);
                    this.updateMinMax(x + w, y);
                    this.updateMinMax(x + w, y + h);
                    return rectOriginal.call(this, x, y, w, h);
                };

                let fillOriginal = ctx.fill;
                ctx.fill = function () {
                    this.setBoundingBox();
                    return fillOriginal.call(this);
                };

                let drawImageOriginal = ctx.drawImage;
                ctx.drawImage = function () {
                    this.pathBBox = {};
                    this.pathBBox.minX = this.pathBBox.minY = -99999999999;
                    this.pathBBox.maxX = this.pathBBox.maxY = 99999999999;
                    this.setBoundingBox();
                    return drawImageOriginal.apply(this, arguments);
                };

                let strokeOriginal = ctx.stroke;
                ctx.stroke = function () {
                    this.setBoundingBox(this.lineWidth / 2 + 3);
                    return strokeOriginal.call(this);
                }

                ctx.setBoundingBox = function (excess) {
                    excess = excess || 0;
                    let x = Math.min(Math.max(0, this.pathBBox.minX - excess), this.canvas.width);
                    let y = Math.min(Math.max(0, this.pathBBox.minY - excess), this.canvas.height);
                    let mx = Math.min(Math.max(0, this.pathBBox.maxX + excess), this.canvas.width);
                    let my = Math.min(Math.max(0, this.pathBBox.maxY + excess), this.canvas.height);
                    this.boundingBox = {x: x, y: y, w: mx - x, h: my - y};
                };

                ctx.popBoundingBox = function () {
                    let result = this.boundingBox;
                    this.boundingBox = null;
                    if(result != null){
                        result.x=Math.floor(result.x);
                        result.y=Math.floor(result.y);
                        result.w=Math.ceil(result.w);
                        result.h=Math.ceil(result.h);
                    }
                    return result;
                }
            }
        }

        function applyXorModeComposition(dest, src, xor, bbox) {
            if (bbox == null || bbox.w == 0 || bbox.h == 0) {
                return;
            }
            let start = new Date().getTime();
            let destData = dest.getImageData(bbox.x, bbox.y, bbox.w, bbox.h);
            let srcData = src.getImageData(bbox.x, bbox.y, bbox.w, bbox.h);
            for (let i = 0; i < destData.data.length / 4; i++) {
                if (srcData.data[4 * i + 3] > 0) {
                    destData.data[4 * i] = srcData.data[4 * i] ^ xor.r ^ destData.data[4 * i];    // RED (0-255)
                    destData.data[4 * i + 1] = srcData.data[4 * i + 1] ^ xor.g ^ destData.data[4 * i + 1];    // GREEN (0-255)
                    destData.data[4 * i + 2] = srcData.data[4 * i + 2] ^ xor.b ^ destData.data[4 * i + 2];    // BLUE (0-255)
                    destData.data[4 * i + 3] = destData.data[4 * i + 3];  // APLHA (0-255)
                }
            }
            dest.putImageData(destData, bbox.x, bbox.y);
            if (config.logDebug){
                console.log('DirectDraw DEBUG xormode - composition pixelsize:'+ (bbox.w* bbox.h) +' duration(ms): ' + (new Date().getTime() - start));
            }
        }
		function iprtGraphicsDispose(id, imageContext) {
			delete imageContext.graphicsStates[id];
		}

        function iprtGraphicsSwitch(ctx, id, imageContext) {
            let graphicsStates = imageContext.graphicsStates;
            if (graphicsStates[id] != null) {
                setCtxState(graphicsStates[id], ctx);
            } else {
                console.log("Graphics with id " + id + " not initialized!");
            }
            imageContext.currentStateId = id;
        }

        function setCtxState(graphicsState, ctx) {
            if (graphicsState != null) {
                if (graphicsState.strokeArgs != null) {
                    iprtSetStroke(ctx, graphicsState.strokeArgs);
                }
                if (graphicsState.paintArgs != null) {
                    iprtSetPaint(ctx, graphicsState.paintArgs);
                }
                if (graphicsState.compositeArgs != null) {
                    iprtSetComposite(ctx, graphicsState.compositeArgs);
                }
                if (graphicsState.fontArgs != null) {
                    iprtSetFont(ctx, graphicsState.fontArgs);
                }
                if (graphicsState.transform != null) {
                    let t = graphicsState.transform;
                    ctx.setTransform(t[0], t[1], t[2], t[3], t[4], t[5]);
                } else {
                    let iT = identityTransform();
                    ctx.setTransform(iT[0], iT[1], iT[2], iT[3], iT[4], iT[5]);
                }
            }
        }

        function iprtGraphicsCreate(ctx, id, args, imageContext) {
            let graphicsStates = imageContext.graphicsStates;
            if (graphicsStates[id] == null) {
                graphicsStates[id] = {};
                imageContext.currentStateId = id;
                args.shift();
                graphicsStates[id].transform = iprtSetTransform(ctx, args);
                args.shift();
                iprtSetStroke(ctx, args);
                graphicsStates[id].strokeArgs = args.slice(0, 1);
                args.shift();
                iprtSetComposite(ctx, args);
                graphicsStates[id].compositeArgs = args.slice(0, 1);
                args.shift();
                iprtSetPaint(ctx, args);
                graphicsStates[id].paintArgs = args.slice(0, 1);
                args.shift();
                graphicsStates[id].fontArgs = args;
                graphicsStates[id].fontTransform = iprtSetFont(ctx, args);
            } else {
                console.log("Graphics with id " + id + " already exist!");
            }
        }

        function iprtDraw(ctx, args, transform) {
            ctx.save();
            if (path(ctx, args[1])) {
                ctx.clip(fillRule(args[1]));
            }
            path(ctx, args[0], true, transform);
            ctx.stroke();
            ctx.restore();
        }

        function iprtFill(ctx, args) {
            ctx.save();
            if (path(ctx, args[1])) {
                ctx.clip(fillRule(args[1]));
            }
            path(ctx, args[0]);
            ctx.fill(fillRule(args[0]));
            ctx.restore();
        }

        function iprtDrawImage(ctx, args) {
            ctx.save();
            let image = args[0].image.data;
            let transform = args[1];
            let crop = args[2];
            let bgcolor = args[3];
            let clip = args[4];

            if (path(ctx, clip)) {
                ctx.clip(fillRule(clip));
            }
            if (transform != null) {
                iprtTransform(ctx, [transform]);
            }
            if (bgcolor != null) {
                ctx.fillStyle = parseColor(bgcolor.color.rgba);
                ctx.beginPath();
                if (crop == null) {
                    ctx.rect(0, 0, image.width, image.height);
                } else {
                    ctx.rect(0, 0, crop.rectangle.w, crop.rectangle.h);
                }
                ctx.fill();
            }
            if (crop == null) {
                ctx.drawImage(image, 0, 0);
            } else {
                crop = crop.rectangle;
                ctx.drawImage(image, crop.x, crop.y, crop.w, crop.h, 0, 0, crop.w, crop.h);
            }
            ctx.restore();
        }

        function iprtDrawWebImage(ctx, args, webImageData) {
            let transform = args[0];
            let crop = args[1];
            let bgcolor = args[2];
            let clip = args[3];

            let buffer = canvasBuffer.pop();
            return drawBin(webImageData, buffer).then(function (imageCanvas) {
                ctx.save();
                let dpr = config.dpr;
                if (path(ctx, clip)) {
                    ctx.clip(fillRule(clip));
                }
                if (transform != null) {
                    iprtTransform(ctx, [transform]);
                }
                if (bgcolor != null) {
                    ctx.fillStyle = parseColor(bgcolor.color.rgba);
                    ctx.beginPath();
                    if (crop == null) {
                        ctx.rect(0, 0, imageCanvas.width / dpr, imageCanvas.height / dpr);
                    } else {
                        ctx.rect(0, 0, crop.rectangle.w, crop.rectangle.h);
                    }
                    ctx.fill();
                }
                if (crop == null) {
                    ctx.drawImage(imageCanvas, 0, 0, imageCanvas.width, imageCanvas.height, 0, 0, imageCanvas.width / dpr, imageCanvas.height / dpr);
                } else {
                    crop = crop.rectangle;
                    ctx.drawImage(imageCanvas, crop.x * dpr, crop.y * dpr, crop.w * dpr, crop.h * dpr, 0, 0, crop.w, crop.h);
                }
                ctx.restore();

                imageCanvas.width = 0;//clear the buffer image for future reuse
                canvasBuffer.push(imageCanvas);
            });
        }

        function iprtDrawGlyphList(ctx, args) {
            let combinedArgs = resolveArgs(args[0].combined.ids, constantPoolCache);
            let size = combinedArgs[0].points;
            let points = combinedArgs[1].points;
            let glyphs = combinedArgs.slice(2);
            let clip = args[1];
            ctx.save();
            if (path(ctx, clip)) {
                ctx.clip(fillRule(clip));
            }
            if (glyphs.length > 0) {
                let buffer = document.createElement("canvas");
                buffer.width = size.points[2];
                buffer.height = size.points[3];
                let bufctx = buffer.getContext("2d");
                for (let i = 0; i < glyphs.length; i++) {
                    if (glyphs[i].glyph.data != null) {
                        let img = glyphs[i].glyph.data;
                        let x = points.points[i * 2];
                        let y = points.points[i * 2 + 1];
                        bufctx.drawImage(img, 0, 0, img.width, img.height, x, y, img.width, img.height);
                    }
                }
                bufctx.fillStyle = ctx.fillStyle;
                bufctx.globalCompositeOperation = 'source-in';
                bufctx.fillRect(0, 0, buffer.width, buffer.height);
                ctx.drawImage(buffer, 0, 0, buffer.width, buffer.height, size.points[0], size.points[1], buffer.width, buffer.height);
            }
            ctx.restore();
        }

        function iprtDrawString(ctx, args, fontTransform) {
            let string = args[0].string;
            let p = args[1].points.points;
            let x=p[0];
            let y=p[1];
            let clip = args[2];
            ctx.save();
            if (path(ctx, clip)) {
                ctx.clip(fillRule(clip));
            }
            if (fontTransform != null) {
                let t = fontTransform;
                ctx.transform(t.m00, t.m10, t.m01, t.m11, t.m02 + x, t.m12 + y);
                ctx.fillText(string, 0, 0);
            } else {
                var currentX=x;
                for (var i = 0;i<string.length;i++){
                    if(p[i+2]===0){
                        continue;
                    }
                    let c = getCharGroup(i,string,p);
                    var canvasWidth = ctx.measureText(c).width;
                    ctx.save();
                    var scaleX = p[i+2] / canvasWidth;
                    if(scaleX<=1){
                       ctx.scale(scaleX, 1);
                       ctx.fillText(c, currentX/scaleX, y);
                    }else{
                       ctx.fillText(c, currentX+((p[i+2] - canvasWidth)/2), y);
                    }
                    ctx.restore();
                    currentX+=p[i+2];
                }
            }
            ctx.restore();
        }

        function getCharGroup(i, value, points){
            let c = value.charAt(i);
            let currentIndex = i+1;
            while(value.length>currentIndex && points[currentIndex+2]===0){
                c+=value.charAt(currentIndex);
                currentIndex++
            }
            return c;
        }

        function iprtSetFont(ctx, args) {
            if (args[0] == null) {
                return ctx.font;
            }
            let font = args[0].font;
            let style = '';
            switch (font.style) {
                case StyleProto.NORMAL:
                    style = '';
                    break;
                case StyleProto.OBLIQUE:
                    style = 'bold';
                    break;
                case StyleProto.ITALIC:
                    style = 'italic';
                    break;
                case StyleProto.BOLDANDITALIC:
                    style = 'bold italic';
                    break;
            }
            let fontFamily = font.family;
            if (font.family !== 'sans-serif' && font.family !== 'serif' && font.family !== 'monospace') {
                fontFamily = "\"" + ctxId + font.family + "\"";
            }
            ctx.font = style + " " + font.size + "px " + fontFamily;
            return font.transform;
        }

        function iprtCopyArea(ctx, args) {
            let p = args[0].points.points;
            let clip = args[1];
            let dpr = config.dpr;
            ctx.save();

            if (path(ctx, clip)) {
                ctx.clip(fillRule(clip));
            }
            ctx.beginPath();
            ctx.setTransform(1, 0, 0, 1, 0, 0);
            ctx.rect(p[0] * dpr, p[1] * dpr, p[2] * dpr, p[3] * dpr);
            ctx.clip();
            ctx.translate(p[4] * dpr, p[5] * dpr);
            ctx.drawImage(ctx.canvas, 0, 0);
            ctx.restore();
        }

        function iprtTransform(ctx, args) {
            let t = args[0].transform;
            ctx.transform(t.m00, t.m10, t.m01, t.m11, t.m02, t.m12);
            return [t.m00, t.m10, t.m01, t.m11, t.m02, t.m12];
        }

        function iprtSetTransform(ctx, args) {
            let t = args[0].transform;
            t = withDpr([t.m00, t.m10, t.m01, t.m11, t.m02, t.m12]);
            ctx.setTransform(t[0], t[1], t[2], t[3], t[4], t[5]);
            return t;
        }

        function iprtSetStroke(ctx, args) {
            let stroke = args[0].stroke;
            ctx.lineWidth = stroke.width;
            ctx.miterLimit = stroke.miterLimit;
            switch (stroke.cap) {
                case StrokeCapProto.CAP_BUTT:
                    ctx.lineCap = "butt";
                    break;
                case StrokeCapProto.CAP_ROUND:
                    ctx.lineCap = "round";
                    break;
                case StrokeCapProto.CAP_SQUARE:
                    ctx.lineCap = "square";
                    break;
            }
            switch (stroke.join) {
                case StrokeJoinProto.JOIN_MITER:
                    ctx.lineJoin = "miter";
                    break;
                case StrokeJoinProto.JOIN_ROUND:
                    ctx.lineJoin = "round";
                    break;
                case StrokeJoinProto.JOIN_BEVEL:
                    ctx.lineJoin = "bevel";
                    break;
            }
            if (stroke.dash != null) {
                if (ctx.setLineDash != null) {//ie10 does fails on dash
                    ctx.setLineDash(stroke.dash);
                    ctx.lineDashOffset = stroke.dashOffset;
                }
            }
        }

        function iprtSetPaint(ctx, args) {
            let constant = args[0];
            if (constant.color != null) {
                let color = parseColor(constant.color.rgba);
                ctx.fillStyle = color;
                ctx.strokeStyle = color;
            } else if (constant.texture != null) {
                let anchor = constant.texture.anchor;
                let preloadedImage = constant.texture.image.data;
                let ptrn;
                if (anchor.x == 0 && anchor.y == 0 && anchor.w == preloadedImage.width && anchor == preloadedImage.height) {
                    ptrn = ctx.createPattern(preloadedImage, 'repeat');
                } else {
                    let ptrnCanvas = document.createElement('canvas');
                    let ax = anchor.x < 0 ? ((anchor.x % anchor.w) + anchor.w) : (anchor.x % anchor.w);
                    let ay = anchor.y < 0 ? ((anchor.y % anchor.h) + anchor.h) : (anchor.y % anchor.h);
                    ptrnCanvas.width = anchor.w;
                    ptrnCanvas.height = anchor.h;
                    let ptrnContext = ptrnCanvas.getContext("2d");
                    ptrnContext.fillRect(0, 0, anchor.w, anchor.h);
                    ptrnContext.fillStyle = ptrnContext.createPattern(preloadedImage, 'repeat');
                    ptrnContext.setTransform(anchor.w / preloadedImage.width, 0, 0, anchor.h / preloadedImage.height, ax, ay);
                    ptrnContext.fillRect(-ax * preloadedImage.width / anchor.w, -ay * preloadedImage.height / anchor.h, preloadedImage.width,
                        preloadedImage.height);
                    ptrn = ctx.createPattern(ptrnCanvas, 'repeat');
                }
                ctx.fillStyle = ptrn;
                ctx.strokeStyle = ptrn;
            } else if (constant.linearGrad != null) {
                let gradient = iprtLinearGradient(ctx, constant.linearGrad);
                ctx.fillStyle = gradient;
                ctx.strokeStyle = gradient;
            } else if (constant.radialGrad != null) {
                let gradient = iprtRadialGradient(ctx, constant.radialGrad);
                ctx.fillStyle = gradient;
                ctx.strokeStyle = gradient;
            }
        }

        function iprtLinearGradient(ctx, g) {
            let x0 = g.xStart;
            let y0 = g.yStart;
            let dx = g.xEnd - x0;
            let dy = g.yEnd - y0;
            // in case of cyclic gradient calculate repeat counts
            let repeatCount = 1, increaseCount = repeatCount, decreaseCount = 0;
            if (g.repeat != CyclicMethodProto.NO_CYCLE && (dx != 0 || dy != 0)) {
                // calculate how many times gradient will completely repeat in both directions until it touches canvas corners
                let c = ctx.canvas;
                let times = [calculateTimes(x0, y0, dx, dy, 0, 0),
                    calculateTimes(x0, y0, dx, dy, c.width, 0),
                    calculateTimes(x0, y0, dx, dy, c.width, c.height),
                    calculateTimes(x0, y0, dx, dy, 0, c.height)];
                // increase count is maximum of all positive times rounded up
                increaseCount = Math.ceil(Math.max.apply(Math, times));
                // decrease count is maximum of all negative times rounded up (with inverted sign)
                decreaseCount = Math.ceil(-Math.min.apply(Math, times));
                repeatCount = increaseCount + decreaseCount;
            }
            let gradient = ctx.createLinearGradient(x0 - dx * decreaseCount, y0 - dy * decreaseCount, x0 + dx * increaseCount, y0 + dy * increaseCount);
            for (let rep = -decreaseCount, offset = 0; rep < increaseCount; rep++, offset++) {
                if (g.repeat != CyclicMethodProto.REFLECT || rep % 2 == 0) {
                    for (let i = 0; i < g.colors.length; i++) {
                        gradient.addColorStop((offset + g.fractions[i]) / repeatCount, parseColor(g.colors[i]));
                    }
                } else {
                    // reflect colors
                    for (let i = g.colors.length - 1; i >= 0; i--) {
                        gradient.addColorStop((offset + (1 - g.fractions[i])) / repeatCount, parseColor(g.colors[i]));
                    }
                }
            }
            return gradient;
        }

        // calculates how many times vector (dx, dy) will repeat from (x0, y0) until it touches a straight line
        // which goes through (x1, y1) and perpendicular to the vector
        function calculateTimes(x0, y0, dx, dy, x1, y1) {
            return ((x1 - x0) * dx + (y1 - y0) * dy) / (dx * dx + dy * dy);
        }
		
        function iprtRadialGradient(ctx, g) {
            fixFocusPoint(g);
            let fX = g.xFocus;
            let fY = g.yFocus;
            let dx = g.xCenter - fX;
            let dy = g.yCenter - fY;
            let r = g.radius;
            // in case of cyclic gradient calculate repeat counts
            let repeatCount = 1;
            if (g.repeat != CyclicMethodProto.NO_CYCLE) {
                if (dx == 0 && dy == 0) {
                    // calculate how many times gradient will completely repeat in both directions until it touches canvas corners
                    let c = ctx.canvas;
                    let times = [getDistance(fX, fY, 0, 0) / r,
                        getDistance(fX, fY, c.width, 0) / r,
                        getDistance(fX, fY, c.width, c.height) / r,
                        getDistance(fX, fY, 0, c.height) / r];
                    repeatCount = Math.ceil(Math.max.apply(Math, times));
                } else {
                    let distance = Math.sqrt(dx * dx + dy * dy);
                    // calculate vector which goes from focus point through center to the circle bound
                    let vdX = dx + r * dx / distance;
                    let vdY = dy + r * dy / distance;
                    // and in opposite direction
                    let ovdX = dx - r * dx / distance;
                    let ovdY = dy - r * dy / distance;
                    // calculate how many times gradient will completely repeat in both directions until it touches canvas corners
                    let c = ctx.canvas;
                    let times = [calculateTimes(fX, fY, vdX, vdY, 0, 0),
                        calculateTimes(fX, fY, vdX, vdY, c.width, 0),
                        calculateTimes(fX, fY, vdX, vdY, c.width, c.height),
                        calculateTimes(fX, fY, vdX, vdY, 0, c.height),
                        calculateTimes(fX, fY, ovdX, ovdY, 0, 0),
                        calculateTimes(fX, fY, ovdX, ovdY, c.width, 0),
                        calculateTimes(fX, fY, ovdX, ovdY, c.width, c.height),
                        calculateTimes(fX, fY, ovdX, ovdY, 0, c.height)];
                    repeatCount = Math.ceil(Math.max.apply(Math, times));
                }
            }
            // in case of repeat focus stays in the same place, radius and distance between focus and center are multiplied
            let gradient = ctx.createRadialGradient(fX, fY, 0, fX + repeatCount * dx, fY + repeatCount * dy, r * repeatCount);
            for (let rep = 0; rep < repeatCount; rep++) {
                if (g.repeat != CyclicMethodProto.REFLECT || rep % 2 == 0) {
                    for (let i = 0; i < g.colors.length; i++) {
                        gradient.addColorStop((rep + g.fractions[i]) / repeatCount, parseColor(g.colors[i]));
                    }
                } else {
                    // reflect colors
                    for (let i = g.colors.length - 1; i >= 0; i--) {
                        gradient.addColorStop((rep + (1 - g.fractions[i])) / repeatCount, parseColor(g.colors[i]));
                    }
                }
            }
            return gradient;
        }

        // fix gradient focus point as java does
        function fixFocusPoint(gradient) {
            let dx = gradient.xFocus - gradient.xCenter;
            let dy = gradient.yFocus - gradient.yCenter;
            if (dx == 0 && dy == 0) {
                return;
            }
            let scaleBack = 0.99;
            let radiusSq = gradient.radius * gradient.radius;
            let distSq = (dx * dx) + (dy * dy);
            // test if distance from focus to center is greater than the radius
            if (distSq > radiusSq * scaleBack) {
                // clamp focus to radius
                let scale = Math.sqrt(radiusSq * scaleBack / distSq);
                // modify source object to skip fixes later
                gradient.xFocus = gradient.xCenter + dx * scale;
                gradient.yFocus = gradient.yCenter + dy * scale;
            }
        }

        function getDistance(x0, y0, x1, y1) {
            return Math.sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0));
        }

        function iprtSetComposite(ctx, args) {
            let composite = args[0].composite;
            if (composite != null) {
                ctx.globalAlpha = composite.alpha;
                switch (composite.type) {
                    case CompositeTypeProto.CLEAR:
                        ctx.globalCompositeOperation = "destination-out";
                        ctx.globalAlpha = 1;
                        break;
                    case CompositeTypeProto.SRC:
                        ctx.globalCompositeOperation = "source-over";
                        break;
                    case CompositeTypeProto.DST:
                        ctx.globalCompositeOperation = "destination-over";
                        ctx.globalAlpha = 0;
                        break;
                    case CompositeTypeProto.SRC_OVER:
                        ctx.globalCompositeOperation = "source-over";
                        break;
                    case CompositeTypeProto.DST_OVER:
                        ctx.globalCompositeOperation = "destination-over";
                        break;
                    case CompositeTypeProto.SRC_IN:
                        ctx.globalCompositeOperation = "source-in";
                        break;
                    case CompositeTypeProto.DST_IN:
                        ctx.globalCompositeOperation = "destination-in";
                        break;
                    case CompositeTypeProto.SRC_OUT:
                        ctx.globalCompositeOperation = "source-out";
                        break;
                    case CompositeTypeProto.DST_OUT:
                        ctx.globalCompositeOperation = "destination-out";
                        break;
                    case CompositeTypeProto.SRC_ATOP:
                        ctx.globalCompositeOperation = "source-atop";
                        break;
                    case CompositeTypeProto.DST_ATOP:
                        ctx.globalCompositeOperation = "destination-atop";
                        break;
                    case CompositeTypeProto.XOR:
                        ctx.globalCompositeOperation = "xor";
                        break;
                    case CompositeTypeProto.XOR_MODE://handled with custom pixel processing (no effect)
                        ctx.globalCompositeOperation = "source-over";
                        break;
                }
            }
        }

        function path(ctx, arg, biased, transform) {
            if (arg == null) {
                return false;
            }

            let bias = calculateBias(ctx, biased, transform);

            if (arg.rectangle != null) {
                ctx.beginPath();
                pathRectangle(ctx, arg.rectangle, bias);
                return true;
            }

            if (arg.roundRectangle != null) {
                ctx.beginPath();
                pathRoundRectangle(ctx, arg.roundRectangle, bias);
                return true;
            }

            if (arg.ellipse != null) {
                ctx.beginPath();
                pathEllipse(ctx, arg.ellipse, bias);
                return true;
            }

            if (arg.arc != null) {
                ctx.beginPath();
                pathArc(ctx, arg.arc, bias);
                return true;
            }

            // generic path
            if (arg.path != null) {
                ctx.beginPath();
                let path = arg.path;
                let off = 0;
                path.type.forEach(function (type, index) {
                    switch (type) {
                        case SegmentTypeProto.MOVE:
                            ctx.moveTo(path.points[off + 0] + bias.x, path.points[off + 1] + bias.y);
                            off += 2;
                            break;
                        case SegmentTypeProto.LINE:
                            ctx.lineTo(path.points[off + 0] + bias.x, path.points[off + 1] + bias.y);
                            off += 2;
                            break;
                        case SegmentTypeProto.QUAD:
                            ctx.quadraticCurveTo(path.points[off + 0] + bias.x, path.points[off + 1] + bias.y,
                                path.points[off + 2] + bias.x, path.points[off + 3] + bias.y);
                            off += 4;
                            break;
                        case SegmentTypeProto.CUBIC:
                            ctx.bezierCurveTo(path.points[off + 0] + bias.x, path.points[off + 1] + bias.y,
                                path.points[off + 2] + bias.x, path.points[off + 3] + bias.y,
                                path.points[off + 4] + bias.x, path.points[off + 5] + bias.y);
                            off += 6;
                            break;
                        case SegmentTypeProto.CLOSE:
                            ctx.closePath();
                            break;
                        default:
                            console.log("segment.type:" + segment.type + " not recognized");
                    }
                });
                return true;
            }
            return false;
        }

        function pathRectangle(ctx, rect, bias) {
            ctx.rect(rect.x + bias.x, rect.y + bias.y, rect.w, rect.h);
        }

        function pathEllipse(ctx, elli, bias) {
            let kappa = 0.5522847498307933;
            let pcv = 0.5 + kappa * 0.5;
            let ncv = 0.5 - kappa * 0.5;

            ctx.moveTo(elli.x + bias.x + elli.w, elli.y + bias.y + 0.5 * elli.h);
            let pts = getEllipseCoords([1.0, pcv, pcv, 1.0, 0.5, 1.0], elli, bias);
            ctx.bezierCurveTo(pts[0], pts[1], pts[2], pts[3], pts[4], pts[5]);
            pts = getEllipseCoords([ncv, 1.0, 0.0, pcv, 0.0, 0.5], elli, bias);
            ctx.bezierCurveTo(pts[0], pts[1], pts[2], pts[3], pts[4], pts[5]);
            pts = getEllipseCoords([0.0, ncv, ncv, 0.0, 0.5, 0.0], elli, bias);
            ctx.bezierCurveTo(pts[0], pts[1], pts[2], pts[3], pts[4], pts[5]);
            pts = getEllipseCoords([pcv, 0.0, 1.0, ncv, 1.0, 0.5], elli, bias);
            ctx.bezierCurveTo(pts[0], pts[1], pts[2], pts[3], pts[4], pts[5]);
            ctx.closePath();
        }

        function getEllipseCoords(pts, elli, bias) {
            pts[0] = elli.x + bias.x + pts[0] * elli.w;
            pts[1] = elli.y + bias.y + pts[1] * elli.h;
            pts[2] = elli.x + bias.x + pts[2] * elli.w;
            pts[3] = elli.y + bias.y + pts[3] * elli.h;
            pts[4] = elli.x + bias.x + pts[4] * elli.w;
            pts[5] = elli.y + bias.y + pts[5] * elli.h;
            return pts;
        }

        function pathRoundRectangle(ctx, rr, bias) {
            let acv = 0.22385762508460333;

            let pts = getRRCoords([0, 0, 0, 0.5], rr, bias);
            ctx.moveTo(pts[0], pts[1]);

            pts = getRRCoords([0, 0, 1, -0.5], rr, bias);
            ctx.lineTo(pts[0], pts[1]);
            pts = getRRCoords([0, 0, 1, -acv, 0, acv, 1, 0, 0, 0.5, 1, 0], rr, bias);
            ctx.bezierCurveTo(pts[0], pts[1], pts[2], pts[3], pts[4], pts[5]);

            pts = getRRCoords([1, -0.5, 1, 0], rr, bias);
            ctx.lineTo(pts[0], pts[1]);
            pts = getRRCoords([1, -acv, 1, 0, 1, 0, 1, -acv, 1, 0, 1, -0.5], rr, bias);
            ctx.bezierCurveTo(pts[0], pts[1], pts[2], pts[3], pts[4], pts[5]);

            pts = getRRCoords([1, 0, 0, 0.5], rr, bias);
            ctx.lineTo(pts[0], pts[1]);
            pts = getRRCoords([1, 0, 0, acv, 1, -acv, 0, 0, 1, -0.5, 0, 0], rr, bias);
            ctx.bezierCurveTo(pts[0], pts[1], pts[2], pts[3], pts[4], pts[5]);

            pts = getRRCoords([0, 0.5, 0, 0], rr, bias);
            ctx.lineTo(pts[0], pts[1]);
            pts = getRRCoords([0, acv, 0, 0, 0, 0, 0, acv, 0, 0, 0, 0.5], rr, bias);
            ctx.bezierCurveTo(pts[0], pts[1], pts[2], pts[3], pts[4], pts[5]);

            ctx.closePath();
        }

        function getRRCoords(pts, rr, bias) {
            let coords = [];
            let nc = 0;
            for (let i = 0; i < pts.length; i += 4) {
                coords[nc++] = rr.x + bias.x + pts[i + 0] * rr.w + pts[i + 1] * Math.abs(rr.arcW);
                coords[nc++] = rr.y + bias.y + pts[i + 2] * rr.h + pts[i + 3] * Math.abs(rr.arcH);
            }
            return coords;
        }

        function pathArc(ctx, arc, bias) {
            let w = arc.w / 2, h = arc.h / 2, x = arc.x + bias.x + w, y = arc.y + bias.y + h;
            let angStRad = -(arc.start * Math.PI / 180);
            let ext = -arc.extent;
            let arcSegs = 4;
            let increment = ext < 0 ? Math.PI / 2 : -Math.PI / 2;
            let cv = ext < 0 ? 0.5522847498307933 : -0.5522847498307933;
            if (ext > -360 && ext < 360) {
                arcSegs = Math.ceil(Math.abs(ext) / 90);
                increment = (ext / arcSegs) * Math.PI / 180;
                cv = 4.0 / 3.0 * Math.sin(increment / 2) / (1.0 + Math.cos(increment / 2));
                arcSegs = cv == 0 ? 0 : arcSegs;
            }
            ctx.moveTo(x + Math.cos(angStRad) * w, y + Math.sin(angStRad) * h);
            for (let i = 0; i < arcSegs; i++) {
                let angle = angStRad + increment * i;
                let relx = Math.cos(angle);
                let rely = Math.sin(angle);
                let pts = [];
                pts[0] = (x + (relx - cv * rely) * w);
                pts[1] = (y + (rely + cv * relx) * h);
                angle += increment;
                relx = Math.cos(angle);
                rely = Math.sin(angle);
                pts[2] = (x + (relx + cv * rely) * w);
                pts[3] = (y + (rely - cv * relx) * h);
                pts[4] = (x + relx * w);
                pts[5] = (y + rely * h);
                ctx.bezierCurveTo(pts[0], pts[1], pts[2], pts[3], pts[4], pts[5]);
            }
            switch (arc.type) {
                case ArcTypeProto.OPEN:
                    break;
                case ArcTypeProto.CHORD:
                    ctx.closePath();
                    break;
                case ArcTypeProto.PIE:
                    ctx.lineTo(x, y);
                    ctx.closePath();
                    break;
            }
        }

        function calculateBias(ctx, biased, transform) {
            if (!biased) {
                return {x: 0, y: 0};
            } else {
                return {
                    x: (ctx.lineWidth * transform[0]) & 1 ? 0.5 / transform[0] : 0,
                    y: (ctx.lineWidth * transform[3]) & 1 ? 0.5 / transform[3] : 0
                }
            }
        }

        function parseColor(rgba) {
            let samles = parseColorSamples(rgba);
            return 'rgba(' + samles.r + ',' + samles.g + ',' + samles.b + ',' + (samles.a / 255) + ')';
        }

        function parseColorSamples(rgba) {
            let mask = 0x000000FF;
            return {
                r: ((rgba >>> 24) & mask),
                g: ((rgba >>> 16) & mask),
                b: ((rgba) >>> 8 & mask),
                a: (rgba & mask)
            };
        }

        function prepareImages(images) {
            return new Promise(function (resolve, reject) {
                try {
                    prepareImagesInternal(images, resolve, reject);
                } catch (e) {
                    config.onErrorMessage(error);
                    reject(e);
                }
            });
        }

        function initializeFontFaces(fontFaces) {
            return new Promise(function (resolve, reject) {
                try {
                    if (fontFaces.length > 0) {
                        let loadedFonts = fontFaces.map(function (fontFace) {
                            return new Promise(function (resolve) {
                                if (fontsArray.indexOf(fontFace.name) >= 0) {
                                    resolve();
                                } else {
                                    fontsArray.push(fontFace.name);
                                    let fontCss = document.createElement("style");
                                    fontCss.type = "text/css";
                                    fontCss.setAttribute("data-dd-ctx", ctxId);
                                    let fontName = ctxId + fontFace.name;
                                    fontCss.innerHTML = getFontFaceData(fontName, fontFace.font, fontFace.style);
                                    document.body.appendChild(fontCss);
                                    if (isFontAvailable(fontName)) {
                                        resolve();
                                    } else {
                                        setTimeout(resolve, 5);
                                    }
                                }
                            });
                        });
                        Promise.all(loadedFonts).then(resolve);
                    } else {
                        resolve();
                    }
                } catch (e) {
                    config.onErrorMessage(error);
                    reject(e);
                }
            });
        }

        function isFontAvailable(fontName) {
            let canvas = document.createElement("canvas");
            let context = canvas.getContext("2d");
            let text = "abcdefghijklmnopqrstuvwxyz0123456789";
            context.font = "72px monospace";
            let baselineSize = context.measureText(text).width;
            context.font = "72px '" + fontName + "', monospace";
            let newSize = context.measureText(text).width;
            if (newSize == baselineSize) {
                return false;
            } else {
                return true;
            }
        }

        function getFontFaceData(name, font, style) {
            let fontFaceCss = "@font-face {";
            fontFaceCss += "font-family: '" + name + "';";
            fontFaceCss += "src: url(data:font/truetype;base64," + toBase64(font) + ");";
            if (style != null) {
                fontFaceCss += "font-style: " + style + ";";
            }
            fontFaceCss += "}";
            return fontFaceCss
        }

        function prepareImagesInternal(images, resolve, reject) {
            if (images.length > 0) {
                let loadedImages = images.map(function (image) {
                    return new Promise(function (resolve) {
                        let img = new Image();
                        img.onload = function () {
                            image.data = img;
                            resolve();
                        };
                        img.src = getImageData(image);
                    });
                });
                Promise.all(loadedImages).then(resolve);
            } else {
                resolve();
            }
        }

        function toBase64(data) {
            let binary = '';
            let bytes = new Uint8Array(data.buffer, data.offset, data.limit - data.offset);

            for (let i = 0, l = bytes.byteLength; i < l; i++) {
                binary += String.fromCharCode(bytes[i]);
            }
            return window.btoa(binary);
        }

        function getImageData(image) {
            return "data:image/png;base64," + toBase64(image.data);
        }

        function fillRule(constant) {
            if (constant.path != null) {
                return constant.path.windingOdd ? 'evenodd' : 'nonzero';
            }
            return 'nonzero';
        }

        function resolveArgs(args, cache) {
            let result = [];
            for (let i = 0; i < args.length; i++) {
                result[i] = constantPoolCache[args[i]];
            }
            return result;
        }

        function concatTransform(m, t) {
            let r = [];
            if (m == null) {
                m = identityTransform();
            }
            r[0] = m[0] * t[0] + m[2] * t[1];
            r[1] = m[1] * t[0] + m[3] * t[1];
            r[2] = m[0] * t[2] + m[2] * t[3];
            r[3] = m[1] * t[2] + m[3] * t[3];
            r[4] = m[0] * t[4] + m[2] * t[5] + m[4];
            r[5] = m[1] * t[4] + m[3] * t[5] + m[5];
            return r;
        }

        function withDpr(m) {
            return concatTransform(identityTransform(), m);
        }

        function identityTransform() {
            return [config.dpr, 0, 0, config.dpr, 0, 0]
        }

        function transformPoint(x, y, t) {
            let xt = t[0] * x + t[2] * y + t[4];
            let yt = t[1] * x + t[3] * y + t[5];
            return {x: xt, y: yt};
        }

        function dispose() {
            let styles = document.body.getElementsByTagName("style");
            let toRemove = [];
            for (let i = 0; i < styles.length; i++) {
                if (styles[i].getAttribute("data-dd-ctx") === ctxId) {
                    toRemove.push(styles[i]);
                }
            }
            toRemove.forEach(function (element) {
                document.body.removeChild(element);
            });
        }

        function logRenderTime(startTime, webImage) {
            if (config.logDebug && webImage != null) {
                let time = new Date().getTime() - startTime;
                let instLength = webImage.instructions == null ? 0 : webImage.instructions.length;
                let constLength = webImage.constants == null ? 0 : webImage.constants.length;
                let fontsLength = webImage.fontFaces == null ? 0 : webImage.fontFaces.length;
                console.log("DirectDraw DEBUG render time " + time + "ms (insts:" + instLength + ", consts:" + constLength + ", fonts:" + fontsLength + ")");
            }
        }

        return {
            draw64: draw64,
            drawBin: drawBin,
            drawProto: drawProto,
            dispose: dispose,
            getConstantPoolCache: function () {
                return constantPoolCache;
            }
        };
	}
}
