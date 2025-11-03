// Type definitions for styled-components theme
import 'styled-components';

declare module 'styled-components' {
  export interface DefaultTheme {
    colors: {
      otelBlue: string;
      otelYellow: string;
      otelGray: string;
      otelRed: string;
      backgroundGray: string;
      lightBorderGray: string;
      borderGray: string;
      textGray: string;
      textLightGray: string;
      white: string;
    };
    breakpoints: {
      desktop: string;
    };
    sizes: {
      mxLarge: string;
      mLarge: string;
      mMedium: string;
      mSmall: string;
      dxLarge: string;
      dLarge: string;
      dMedium: string;
      dSmall: string;
      nano: string;
    };
    fonts: {
      bold: string;
      regular: string;
      semiBold: string;
      light: string;
    };
  }
}
