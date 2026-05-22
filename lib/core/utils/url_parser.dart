import '../models/stream_url_model.dart';

class UrlParser {
  /// Parse any input URL and return a StreamUrlModel
  static StreamUrlModel parse(String rawUrl) {
    final url = rawUrl.trim();

    if (url.isEmpty) {
      return StreamUrlModel(
        originalUrl: url,
        resolvedUrl: url,
        type: StreamType.unknown,
      );
    }

    // Check Xtream Codes format: host:port/user/pass (with optional .ts or .m3u8)
    final xtreamResult = _tryParseXtream(url);
    if (xtreamResult != null) return xtreamResult;

    // Check HLS (.m3u8)
    if (_isHls(url)) {
      return StreamUrlModel(
        originalUrl: url,
        resolvedUrl: url,
        type: StreamType.hls,
      );
    }

    // Check TS (.ts)
    if (_isTs(url)) {
      return StreamUrlModel(
        originalUrl: url,
        resolvedUrl: url,
        type: StreamType.ts,
      );
    }

    // Any other http/https
    if (url.startsWith('http://') || url.startsWith('https://')) {
      return StreamUrlModel(
        originalUrl: url,
        resolvedUrl: url,
        type: StreamType.direct,
      );
    }

    return StreamUrlModel(
      originalUrl: url,
      resolvedUrl: url,
      type: StreamType.unknown,
    );
  }

  // ─── Private helpers ──────────────────────────────────────────────────────

  static bool _isHls(String url) =>
      url.toLowerCase().contains('.m3u8');

  static bool _isTs(String url) =>
      url.toLowerCase().contains('.ts');

  /// Detect Xtream Codes format:
  /// http://host:port/user/pass
  /// http://host:port/user/pass.ts
  /// http://host:port/user/pass.m3u8
  static StreamUrlModel? _tryParseXtream(String url) {
    // Must be http/https
    if (!url.startsWith('http://') && !url.startsWith('https://')) {
      return null;
    }

    try {
      final uri = Uri.parse(url);

      // Xtream: path is /username/password or /username/password.ext
      final pathSegments = uri.pathSegments
          .where((s) => s.isNotEmpty)
          .toList();

      // Xtream paths have exactly 2 segments (user + pass)
      // or 3 if it's /live/user/pass or /live/user/pass.ext
      // Standard format: /user/pass or /user/pass.ts or /user/pass.m3u8
      if (pathSegments.length == 2) {
        final pass = pathSegments[1];

        if (pass.endsWith('.m3u8')) {
          // Explicit HLS Xtream
          return StreamUrlModel(
            originalUrl: url,
            resolvedUrl: url,
            type: StreamType.xtreamHls,
          );
        } else if (pass.endsWith('.ts')) {
          // Explicit TS Xtream
          return StreamUrlModel(
            originalUrl: url,
            resolvedUrl: url,
            type: StreamType.xtreamTs,
          );
        } else if (!pass.contains('.')) {
          // Plain Xtream — no extension, ExoPlayer will auto-detect
          return StreamUrlModel(
            originalUrl: url,
            resolvedUrl: url,
            type: StreamType.xtream,
          );
        }
      }

      return null;
    } catch (_) {
      return null;
    }
  }

  /// Check if a URL needs special headers (e.g., Rongintv workers)
  static Map<String, String> defaultHeadersFor(String url) {
    final headers = <String, String>{};
    if (url.contains('workers.dev') || url.contains('rongintv')) {
      headers['User-Agent'] =
          'Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/91.0.4472.120 Mobile Safari/537.36';
      headers['Referer'] = 'https://rongintv.workers.dev/';
    }
    return headers;
  }
}
