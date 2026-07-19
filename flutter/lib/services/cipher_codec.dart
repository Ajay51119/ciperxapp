class CipherCodec {
  static const String separator = '.';

  static String encode(String text, Map<String, String> map) {
    if (text.isEmpty || map.isEmpty) {
      return text;
    }

    return text
        .split('')
        .map((ch) {
          final up = ch.toUpperCase();
          if (map.containsKey(up)) return map[up]!;
          if (ch == ' ') return '|';
          if (ch == '\n') return '\n';
          return ch;
        })
        .join(separator);
  }

  static String decode(String encoded, Map<String, String> map) {
    if (encoded.isEmpty || map.isEmpty) {
      return encoded;
    }

    final reverse = <String, String>{};
    for (final entry in map.entries) {
      reverse[entry.value.toLowerCase()] = entry.key;
    }

    return encoded.split(separator).map((token) {
      if (token == '|') return ' ';
      if (token == '\n') return '\n';
      return reverse[token.toLowerCase()] ?? token;
    }).join();
  }

  static Map<String, String> randomCipher() {
    const symbols = ['@', '#', r'$', '%', '&', '!', '?', '~', '+', '^', '*', '=', '<', '>'];
    final letters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('');
    final map = <String, String>{};

    for (final letter in letters) {
      final num = 100 + (DateTime.now().microsecondsSinceEpoch + letter.codeUnitAt(0)) % 900;
      final symbol = symbols[(num + letter.codeUnitAt(0)) % symbols.length];
      map[letter] = '$num$symbol';
    }

    return map;
  }
}
