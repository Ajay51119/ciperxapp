import 'dart:convert';

class CipherKeyFile {
  CipherKeyFile({
    required this.version,
    required this.name,
    required this.cipher,
    required this.created,
  });

  final int version;
  final String name;
  final Map<String, String> cipher;
  final DateTime created;

  factory CipherKeyFile.generate({
    required String name,
    required Map<String, String> cipher,
  }) {
    return CipherKeyFile(
      version: 1,
      name: name.trim().isEmpty ? 'CipherKey' : name.trim(),
      cipher: Map<String, String>.from(cipher),
      created: DateTime.now().toUtc(),
    );
  }

  factory CipherKeyFile.fromJson(Map<String, dynamic> json) {
    final rawCipher = json['cipher'];
    if (rawCipher is! Map) {
      throw FormatException('Missing cipher map');
    }

    return CipherKeyFile(
      version: json['version'] is int ? json['version'] as int : 1,
      name: (json['name'] ?? 'CipherKey').toString(),
      cipher: rawCipher.map(
        (key, value) => MapEntry(key.toString().toUpperCase(), value.toString()),
      ),
      created: DateTime.tryParse((json['created'] ?? '').toString()) ?? DateTime.now().toUtc(),
    );
  }

  Map<String, dynamic> toJson() => <String, dynamic>{
        'version': version,
        'name': name,
        'cipher': cipher,
        'created': created.toUtc().toIso8601String(),
      };

  String toPrettyJson() => const JsonEncoder.withIndent('  ').convert(toJson());
}
