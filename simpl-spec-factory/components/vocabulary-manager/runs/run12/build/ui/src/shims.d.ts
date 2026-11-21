declare module 'n3/src/N3Parser.js' {
  export default class N3Parser {
    constructor(options?: { format?: string; baseIRI?: string });
    parse(input: string): unknown[];
  }
}

declare module '@primevue/core/base/style' {
  const BaseStyle: { loadCSS: (options?: object) => object };
  export default BaseStyle;
}
