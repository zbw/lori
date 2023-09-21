declare module "*.vue" {
  import Vue from "vue";
  export default Vue;
}
declare module "vue-openapi";
declare module "vue-material";

declare module "*.json" {
  const value: { [key: string]: any };
  export default value;
}
