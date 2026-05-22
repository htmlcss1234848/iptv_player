import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class UrlInputField extends StatelessWidget {
  final TextEditingController controller;
  final VoidCallback onPlay;
  final VoidCallback onClear;

  const UrlInputField({
    super.key,
    required this.controller,
    required this.onPlay,
    required this.onClear,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: const Color(0xFF111827),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(
          color: const Color(0xFF00E5FF).withOpacity(0.3),
          width: 1,
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
            child: Row(
              children: [
                const Icon(Icons.link, color: Color(0xFF00E5FF), size: 16),
                const SizedBox(width: 8),
                Text(
                  'STREAM URL',
                  style: TextStyle(
                    color: const Color(0xFF00E5FF).withOpacity(0.8),
                    fontSize: 11,
                    letterSpacing: 2,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ],
            ),
          ),
          const Divider(height: 1, color: Color(0xFF1F2937)),
          TextField(
            controller: controller,
            style: const TextStyle(
              color: Colors.white,
              fontSize: 14,
              fontFamily: 'monospace',
            ),
            maxLines: 3,
            minLines: 2,
            decoration: InputDecoration(
              hintText:
                  'http://host:port/user/pass\nhttps://example.com/live/index.m3u8',
              hintStyle: TextStyle(
                color: Colors.white.withOpacity(0.2),
                fontSize: 13,
                fontFamily: 'monospace',
              ),
              border: InputBorder.none,
              contentPadding: const EdgeInsets.all(16),
              suffixIcon: ValueListenableBuilder(
                valueListenable: controller,
                builder: (_, value, __) {
                  if (value.text.isEmpty) return const SizedBox();
                  return IconButton(
                    icon: const Icon(Icons.clear, color: Colors.white38),
                    onPressed: onClear,
                  );
                },
              ),
            ),
          ),
          const Divider(height: 1, color: Color(0xFF1F2937)),
          Padding(
            padding: const EdgeInsets.all(12),
            child: Row(
              children: [
                _ActionBtn(
                  icon: Icons.content_paste,
                  label: 'PASTE',
                  onTap: () async {
                    final data = await Clipboard.getData('text/plain');
                    if (data?.text != null) {
                      controller.text = data!.text!;
                    }
                  },
                ),
                const Spacer(),
                _PlayBtn(onPlay: onPlay, controller: controller),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _ActionBtn extends StatelessWidget {
  final IconData icon;
  final String label;
  final VoidCallback onTap;

  const _ActionBtn({
    required this.icon,
    required this.label,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(8),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        child: Row(
          children: [
            Icon(icon, color: Colors.white54, size: 16),
            const SizedBox(width: 6),
            Text(
              label,
              style: const TextStyle(
                color: Colors.white54,
                fontSize: 11,
                letterSpacing: 1.5,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _PlayBtn extends StatelessWidget {
  final VoidCallback onPlay;
  final TextEditingController controller;

  const _PlayBtn({required this.onPlay, required this.controller});

  @override
  Widget build(BuildContext context) {
    return ValueListenableBuilder(
      valueListenable: controller,
      builder: (_, value, __) {
        final enabled = value.text.trim().isNotEmpty;
        return GestureDetector(
          onTap: enabled ? onPlay : null,
          child: AnimatedContainer(
            duration: const Duration(milliseconds: 200),
            padding:
                const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
            decoration: BoxDecoration(
              color: enabled
                  ? const Color(0xFF00E5FF)
                  : const Color(0xFF1F2937),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Row(
              children: [
                Icon(
                  Icons.play_arrow_rounded,
                  color: enabled ? Colors.black : Colors.white24,
                  size: 20,
                ),
                const SizedBox(width: 6),
                Text(
                  'PLAY',
                  style: TextStyle(
                    color: enabled ? Colors.black : Colors.white24,
                    fontSize: 12,
                    fontWeight: FontWeight.bold,
                    letterSpacing: 2,
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}
