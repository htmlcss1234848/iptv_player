enum StreamType {
  hls,       // .m3u8
  ts,        // .ts
  xtream,    // host:port/user/pass
  xtreamTs,  // host:port/user/pass.ts
  xtreamHls, // host:port/user/pass.m3u8
  direct,    // any other http/https
  unknown,
}

class StreamUrlModel {
  final String originalUrl;
  final String resolvedUrl;
  final StreamType type;
  final Map<String, String> headers;
  final String? title;

  const StreamUrlModel({
    required this.originalUrl,
    required this.resolvedUrl,
    required this.type,
    this.headers = const {},
    this.title,
  });

  StreamUrlModel copyWith({
    String? originalUrl,
    String? resolvedUrl,
    StreamType? type,
    Map<String, String>? headers,
    String? title,
  }) {
    return StreamUrlModel(
      originalUrl: originalUrl ?? this.originalUrl,
      resolvedUrl: resolvedUrl ?? this.resolvedUrl,
      type: type ?? this.type,
      headers: headers ?? this.headers,
      title: title ?? this.title,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'url': resolvedUrl,
      'type': type.name,
      'headers': headers,
      'title': title ?? '',
    };
  }

  @override
  String toString() =>
      'StreamUrlModel(type: ${type.name}, url: $resolvedUrl)';
}
