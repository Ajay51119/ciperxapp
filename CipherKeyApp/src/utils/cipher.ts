export type CipherMap = { [letter: string]: string };

export interface KeyFile {
  version: number;
  name: string;
  cipher: CipherMap;
  created: string;
}

export const LETTERS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('');
const TOKEN_SEPARATOR = '.';

export function encodeText(text: string, map: CipherMap): string {
  return text
    .split('')
    .map(ch => {
      const up = ch.toUpperCase();
      if (map[up]) return map[up];
      if (ch === ' ') return '|';
      if (ch === '\n') return '\n';
      return ch;
    })
    .join(TOKEN_SEPARATOR);
}

export function decodeText(encoded: string, map: CipherMap): string {
  const reverseMap: { [code: string]: string } = {};
  Object.entries(map).forEach(([letter, code]) => {
    reverseMap[String(code).toLowerCase()] = letter;
  });

  return encoded
    .split(TOKEN_SEPARATOR)
    .map(token => {
      if (token === '|') return ' ';
      if (token === '\n') return '\n';
      return reverseMap[token.toLowerCase()] ?? token;
    })
    .join('');
}

export function generateKeyFile(name: string, cipher: CipherMap): KeyFile {
  return {
    version: 1,
    name,
    cipher,
    created: new Date().toISOString(),
  };
}

export function keyFileToString(keyFile: KeyFile): string {
  return JSON.stringify(keyFile, null, 2);
}

export function parseKeyFile(content: string): KeyFile | null {
  try {
    const data = JSON.parse(content);
    if (!data.cipher || typeof data.cipher !== 'object') return null;

    return {
      version: Number(data.version || 1),
      name: String(data.name || 'ImportedKey'),
      cipher: data.cipher as CipherMap,
      created: String(data.created || new Date().toISOString()),
    };
  } catch {
    return null;
  }
}

export function randomCipher(): CipherMap {
  const symbols = ['@', '#', '$', '%', '&', '!', '?', '~', '+', '^', '*', '=', '<', '>'];
  const map: CipherMap = {};
  LETTERS.forEach(letter => {
    const num = Math.floor(Math.random() * 900 + 100);
    const sym = symbols[Math.floor(Math.random() * symbols.length)];
    map[letter] = `${num}${sym}`;
  });
  return map;
}
