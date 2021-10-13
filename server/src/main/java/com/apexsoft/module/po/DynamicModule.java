package com.apexsoft.module.po;

public class DynamicModule {
    /**
     * 菜单id
     */
    private String menuid;

    /**
     * 菜单名称
     */
    private String menuname;

    /**
     * logo地址
     */
    private String logourl;

    /**
     * iframe的跳转地址
     */
    private String routeurl;

    /**
     * 跳转类型
     */
    private String routetype;

    public String getMenuid() {
        return menuid;
    }

    public void setMenuid(String menuid) {
        this.menuid = menuid;
    }

    public String getMenuname() {
        return menuname;
    }

    public void setMenuname(String menuname) {
        this.menuname = menuname;
    }

    public String getLogourl() {
        return logourl;
    }

    public void setLogourl(String logourl) {
        this.logourl = logourl;
    }

    public String getRouteurl() {
        return routeurl;
    }

    public void setRouteurl(String routeurl) {
        this.routeurl = routeurl;
    }

    public String getRoutetype() {
        return routetype;
    }

    public void setRoutetype(String routetype) {
        this.routetype = routetype;
    }

    public static Builder newBuilder(){
        return new Builder();
    }

    public static class Builder{
        /**
         * 菜单id
         */
        private String menuid;

        /**
         * 菜单名称
         */
        private String menuname;

        /**
         * logo地址
         */
        private String logourl;

        /**
         * iframe的跳转地址
         */
        private String routeurl;

        /**
         * 跳转类型
         */
        private String routetype;

        public DynamicModule build(){
            DynamicModule result = new DynamicModule();
            result.menuid = this.menuid;
            result.menuname = this.menuname;
            result.logourl = this.logourl;
            result.routeurl = this.routeurl;
            result.routetype = this.routetype;
            return result;
        }

        public Builder menuid(String menuid){
            this.menuid = menuid;
            return this;
        }

        public Builder menuname(String menuname){
            this.menuname = menuname;
            return this;
        }

        public Builder logourl(String logourl){
            this.logourl = logourl;
            return this;
        }

        public Builder routeurl(String routeurl){
            this.routeurl = routeurl;
            return this;
        }

        public Builder routetype(String routetype){
            this.routetype = routetype;
            return this;
        }

    }
}
