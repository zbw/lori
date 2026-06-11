declare module "redoc" {
  export const Redoc: {
    init(
      spec: string | Record<string, unknown>,
      options: Record<string, unknown>,
      element: HTMLElement,
    ): void;
  };
}
