import $ from 'jquery';
import html from './templates/playback.html';
import css from './templates/playback.css';
    
export default class PlaybackModule {
	constructor() {
		let style = $("<style></style>", {
            type : "text/css"
        });
        style.text(css);
        $("head").prepend(style);

        let module = this;
        let api;
        module.injects = api = {
            cfg : 'webswing.config',
            send : 'socket.send',
            getCanvas: 'canvas.get'
        };
        module.provides = {
            playbackInfo : playbackInfo,
            showControls : displayPlaybackBar
        };
        module.ready = function() {
            if (api.cfg.recordingPlayback) {
                displayPlaybackBar();
            }
        }
        let playbackBar;

        function playbackInfo(data) {
            if (playbackBar != null && data.playback != null) {
                data = data.playback;
                let progress = parseInt(data.current / data.total * 100, 10);
                playbackBar.find('.webswing-playback-progress-bar').css('width', progress + '%');
                playbackBar.find('span[data-id="current"]').html(data.current);
                playbackBar.find('span[data-id="total"]').html(data.total)
            }
        }

        function displayPlaybackBar() {
            api.cfg.rootElement.append(html);
            playbackBar = api.cfg.rootElement.find('div[data-id="playbackBar"]');
            playbackBar.find('button[data-id="reset"]').on('click', function(e) {
            	var ctx=api.getCanvas().getContext("2d");
            	ctx.clearRect(0, 0, api.getCanvas().width, api.getCanvas().height);
            	$("canvas.webswing-canvas:not([data-id=canvas])").remove();
            	
                sendCommand('reset');
            });
            playbackBar.find('button[data-id="play"]').on('click', function(e) {
                sendCommand('play');
            })
            playbackBar.find('button[data-id="stop"]').on('click', function(e) {
                sendCommand('stop');
            })
            playbackBar.find('button[data-id="step"]').on('click', function(e) {
                sendCommand('step');
            })
            playbackBar.find('button[data-id="10step"]').on('click', function(e) {
                sendCommand('step10');
            });
            playbackBar.find('button[data-id="100step"]').on('click', function(e) {
                sendCommand('step100');
            });
            playbackBar.show();
        }

        function sendCommand(cmd) {
            api.send({
                playback : {
                    command : cmd
                }
            });
        }

    }
}