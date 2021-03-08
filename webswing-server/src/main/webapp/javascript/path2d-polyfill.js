
/**
 * This is a modified versio of https://github.com/nilzona/path2d-polyfill/blob/master/src/path2d-polyfill.js to support addPath with transform + removed unused functions
 * 
 * Work around for https://developer.microsoft.com/en-us/microsoft-edge/platform/issues/8438884/
 * @ignore
 */

function polyFillPath2D(window) {
  if (typeof window === 'undefined' || !window.CanvasRenderingContext2D) {
    return;
  }
  if (window.Path2D) {
    return;
  }

  /**
   * Crates a Path2D polyfill object
   * @constructor
   * @ignore
   * @param {String} path
   */
  class Path2D {
    constructor(path) {
      this.segments = [];
      if (path && path instanceof Path2D) {
        this.segments.push(...path.segments);
      }
    }

    addPath(path, transform) {
      if (path && path instanceof Path2D) {
        buildPath(this,path.segments,transform);
      }
    }

    moveTo(x, y) {
      this.segments.push(['M', x, y]);
    }

    lineTo(x, y) {
      this.segments.push(['L', x, y]);
    }

    closePath() {
      this.segments.push(['Z']);
    }

    bezierCurveTo(cp1x, cp1y, cp2x, cp2y, x, y) {
      this.segments.push(['C', cp1x, cp1y, cp2x, cp2y, x, y]);
    }

    quadraticCurveTo(cpx, cpy, x, y) {
      this.segments.push(['Q', cpx, cpy, x, y]);
    }

    rect(x, y, width, height) {
        this.segments.push(['R', x, y, width, height]);
    }

  }

  class DOMMatrix  {
    constructor(init) {
      this.e = init?init[4]:0;
      this.f = init?init[5]:0;
    }
  }

  
  function buildPath(canvas, segments, matrix) {
    let dx = matrix?matrix.e:0;
    let dy = matrix?matrix.f:0;
    let x;
    let y;
    let w;
    let h;
    let pathType;
    let cpx;
    let cpy;
    let qcpx;
    let qcpy;
    let startPoint = { x: 0, y: 0 };
    const currentPoint = { x: 0, y: 0 };
    if(canvas.beginPath){
        canvas.beginPath();
    }
    for (let i = 0; i < segments.length; ++i) {
      const s = segments[i];
      pathType = s[0];

      // Reset control point if command is not cubic
      if (
        pathType !== 'C'
      ) {
        cpx = null;
        cpy = null;
      }

      if (
        pathType !== 'Q'
      ) {
        qcpx = null;
        qcpy = null;
      }

      switch (pathType) {
        case 'M':
            x = s[1];
            y = s[2];

          if (pathType === 'M' || !startPoint) {
            startPoint = { x, y };
          }

          canvas.moveTo(x+dx, y+dy);
          break;
        case 'L':
          x = s[1];
          y = s[2];
          canvas.lineTo(x+dx, y+dy);
          break;
        case 'C':
          cpx = s[3]; // Last control point
          cpy = s[4];
          x = s[5];
          y = s[6];
          canvas.bezierCurveTo(s[1]+dx, s[2]+dy, cpx+dx, cpy+dy, x+dx, y+dy);
          break;
        case 'Q':
          qcpx = s[1]; // last control point
          qcpy = s[2];
          x = s[3];
          y = s[4];
          canvas.quadraticCurveTo(qcpx+dx, qcpy+dy, x+dx, y+dy);
          break;
        case 'Z':
          x = startPoint.x;
          y = startPoint.y;
          startPoint = undefined;
          canvas.closePath();
          break;
        case 'R': // rect
          x = s[1];
          y = s[2];
          w = s[3];
          h = s[4];
          startPoint = { x, y };
          canvas.rect(x+dx, y+dy, w, h);
          break;
        default:
        // throw new Error(`${pathType} is not implemented`); ?
      }

      currentPoint.x = x;
      currentPoint.y = y;
    }
  }

  const cFill = window.CanvasRenderingContext2D.prototype.fill;
  const cStroke = window.CanvasRenderingContext2D.prototype.stroke;
  const cClip = window.CanvasRenderingContext2D.prototype.clip;

  window.CanvasRenderingContext2D.prototype.fill = function fill(...args) {
    let fillRule = 'nonzero';
    if (
      args.length === 0 ||
      (args.length === 1 && typeof args[0] === 'string')
    ) {
      cFill.apply(this, args);
      return;
    }
    if (arguments.length === 2) {
      fillRule = args[1];
    }
    const path = args[0];
    buildPath(this, path.segments);
    cFill.call(this, fillRule);
  };
  
  window.CanvasRenderingContext2D.prototype.clip = function clip(...args) {
    let fillRule = 'nonzero';
    if (
      args.length === 0 ||
      (args.length === 1 && typeof args[0] === 'string')
    ) {
      cClip.apply(this, args);
      return;
    }
    if (arguments.length === 2) {
      fillRule = args[1];
    }
    const path = args[0];
    buildPath(this, path.segments);
    cClip.call(this, fillRule);
  };

  window.CanvasRenderingContext2D.prototype.stroke = function stroke(path) {
    if (!path) {
      cStroke.call(this);
      return;
    }
    buildPath(this, path.segments);
    cStroke.call(this);
  };

  window.Path2D = Path2D;
  window.DOMMatrix = DOMMatrix;
  console.log("Path2D polyfill applied");
}
polyFillPath2D(window);