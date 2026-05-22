enum PlayerState {
  idle,
  initializing,
  buffering,
  ready,
  playing,
  paused,
  ended,
  error,
}

class PlayerStateModel {
  final PlayerState state;
  final String? errorMessage;
  final int bufferPercent;
  final Duration position;
  final Duration duration;

  const PlayerStateModel({
    this.state = PlayerState.idle,
    this.errorMessage,
    this.bufferPercent = 0,
    this.position = Duration.zero,
    this.duration = Duration.zero,
  });

  bool get isPlaying => state == PlayerState.playing;
  bool get isBuffering => state == PlayerState.buffering;
  bool get hasError => state == PlayerState.error;
  bool get isIdle => state == PlayerState.idle;

  PlayerStateModel copyWith({
    PlayerState? state,
    String? errorMessage,
    int? bufferPercent,
    Duration? position,
    Duration? duration,
  }) {
    return PlayerStateModel(
      state: state ?? this.state,
      errorMessage: errorMessage ?? this.errorMessage,
      bufferPercent: bufferPercent ?? this.bufferPercent,
      position: position ?? this.position,
      duration: duration ?? this.duration,
    );
  }

  factory PlayerStateModel.fromMap(Map<dynamic, dynamic> map) {
    return PlayerStateModel(
      state: _parseState(map['state'] as String? ?? 'idle'),
      errorMessage: map['error'] as String?,
      bufferPercent: (map['bufferPercent'] as int?) ?? 0,
      position: Duration(milliseconds: (map['positionMs'] as int?) ?? 0),
      duration: Duration(milliseconds: (map['durationMs'] as int?) ?? 0),
    );
  }

  static PlayerState _parseState(String s) {
    switch (s) {
      case 'idle': return PlayerState.idle;
      case 'initializing': return PlayerState.initializing;
      case 'buffering': return PlayerState.buffering;
      case 'ready': return PlayerState.ready;
      case 'playing': return PlayerState.playing;
      case 'paused': return PlayerState.paused;
      case 'ended': return PlayerState.ended;
      case 'error': return PlayerState.error;
      default: return PlayerState.idle;
    }
  }
}
