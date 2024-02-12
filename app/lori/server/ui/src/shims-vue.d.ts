declare module "vue-openapi";
declare module "vue-material";
declare module "*.vue";

declare module "*.json" {
  const value: { [key: string]: any };
  export default value;
}
