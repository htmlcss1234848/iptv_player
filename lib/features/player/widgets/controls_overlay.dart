import 'package:flutter/material.dart';
import '../../../core/models/player_state_model.dart';

class ControlsOverlay extends StatelessWidget {
  final PlayerStateModel state;
  final VoidCallback onPlayPause;
  final VoidCallback onToggleDebug;
  final VoidCallback onBack;
  final VoidCallback onToggleFullscreen;
  final Function(Duration) onSeek;
  final bool isFullscreen;
  final bool debugOn;

  const ControlsOverlay({
    super.key,
    required this.state,
    required this.onPlayPause,
    required this.onToggleDebug,
    required this.onBack,
    required this.onToggleFullscreen,
    required this.onSeek,
    required this.isFullscreen,
    required this.debugOn,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [
            Colors.black.withOpacity(0.7),
            Colors.transparent,
            Colors.transparent,
            Colors.black.withOpacity(0.8),
          ],
          stops: const [0.0, 0.25, 0.75, 1.0],
        ),
      ),
      child: Column(
        children: [
          _TopBar(
            onBack: onBack,
            onToggleDebug: onToggleDebug,
            onToggleFullscreen: onToggleFullscreen,
            isFullscreen: isFullscreen,
            debugOn: debugOn,
            streamType: state.state.name.toUpperCase(),
          ),
          const Spacer(),
          _CenterControls(
            state: state,
            onPlayPause: onPlayPause,
          ),
          const Spacer(),
          _BottomBar(state: state, onSeek: onSeek),
        ],
      ),
    );
  }
}

class _TopBar extends StatelessWidget {
  final VoidCallback onBack;
  final VoidCallback onToggleDebug;
  final VoidCallback onToggleFullscreen;
  final bool isFullscreen;
  final bool debugOn;
  final String streamType;

  const _TopBar({
    required this.onBack,
    required this.onToggleDebug,
    required this.onToggleFullscreen,
    required this.isFullscreen,
    required this.debugOn,
    required this.streamType,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      child: Row(
        children: [
          IconButton(
            icon: const Icon(Icons.arrow_back, color: Colors.white),
            onPressed: onBack,
          ),
          const Spacer(),
          // Debug toggle
          GestureDetector(
            onTap: onToggleDebug,
            child: Container(
              padding:
                  const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
              decoration: BoxDecoration(
                color: debugOn
                    ? const Color(0xFF00E5FF).withOpacity(0.2)
                    : Colors.white.withOpacity(0.1),
                borderRadius: BorderRadius.circular(6),
                border: Border.all(
                  color: debugOn
                      ? const Color(0xFF00E5FF).withOpacity(0.5)
                      : Colors.white24,
                ),
              ),
              child: Text(
                'DEBUG',
                style: TextStyle(
                  color: debugOn
                      ? const Color(0xFF00E5FF)
                      : Colors.white54,
                  fontSize: 10,
                  fontWeight: FontWeight.bold,
                  letterSpacing: 1.5,
                ),
              ),
            ),
          ),
          const SizedBox(width: 8),
          IconButton(
            icon: Icon(
              isFullscreen
                  ? Icons.fullscreen_exit
                  : Icons.fullscreen,
              color: Colors.white,
            ),
            onPressed: onToggleFullscreen,
          ),
        ],
      ),
    );
  }
}

class _CenterControls extends StatelessWidget {
  final PlayerStateModel state;
  final VoidCallback onPlayPause;

  const _CenterControls({required this.state, required this.onPlayPause});

  @override
  Widget build(BuildContext context) {
    if (state.isBuffering || state.state == PlayerState.initializing) {
      return const SizedBox(
        width: 56,
        height: 56,
        child: CircularProgressIndicator(
          color: Color(0xFF00E5FF),
          strokeWidth: 3,
        ),
      );
    }

    return GestureDetector(
      onTap: onPlayPause,
      child: Container(
        width: 64,
        height: 64,
        decoration: BoxDecoration(
          color: Colors.black.withOpacity(0.5),
          shape: BoxShape.circle,
          border: Border.all(color: Colors.white30, width: 2),
        ),
        child: Icon(
          state.isPlaying ? Icons.pause : Icons.play_arrow,
          color: Colors.white,
          size: 36,
        ),
      ),
    );
  }
}

class _BottomBar extends StatelessWidget {
  final PlayerStateModel state;
  final Function(Duration) onSeek;

  const _BottomBar({required this.state, required this.onSeek});

  @override
  Widget build(BuildContext context) {
    final isLive = state.duration == Duration.zero;

    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
      child: Column(
        children: [
          if (!isLive) ...[
            SliderTheme(
              data: SliderTheme.of(context).copyWith(
                activeTrackColor: const Color(0xFF00E5FF),
                inactiveTrackColor: Colors.white24,
                thumbColor: const Color(0xFF00E5FF),
                overlayColor: const Color(0xFF00E5FF).withOpacity(0.2),
                thumbShape:
                    const RoundSliderThumbShape(enabledThumbRadius: 6),
                trackHeight: 3,
              ),
              child: Slider(
                value: state.duration.inMilliseconds > 0
                    ? state.position.inMilliseconds /
                        state.duration.inMilliseconds
                    : 0.0,
                onChanged: (v) {
                  onSeek(Duration(
                    milliseconds:
                        (v * state.duration.inMilliseconds).round(),
                  ));
                },
              ),
            ),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  _formatDuration(state.position),
                  style: const TextStyle(
                      color: Colors.white70, fontSize: 12),
                ),
                Text(
                  _formatDuration(state.duration),
                  style: const TextStyle(
                      color: Colors.white70, fontSize: 12),
                ),
              ],
            ),
          ] else ...[
            // Live indicator
            Row(
              children: [
                Container(
                  width: 8,
                  height: 8,
                  decoration: const BoxDecoration(
                    color: Color(0xFFFF5252),
                    shape: BoxShape.circle,
                  ),
                ),
                const SizedBox(width: 6),
                const Text(
                  'LIVE',
                  style: TextStyle(
                    color: Color(0xFFFF5252),
                    fontSize: 12,
                    fontWeight: FontWeight.bold,
                    letterSpacing: 2,
                  ),
                ),
              ],
            ),
          ],
        ],
      ),
    );
  }

  String _formatDuration(Duration d) {
    final h = d.inHours;
    final m = d.inMinutes.remainder(60).toString().padLeft(2, '0');
    final s = d.inSeconds.remainder(60).toString().padLeft(2, '0');
    if (h > 0) return '$h:$m:$s';
    return '$m:$s';
  }
}
