namespace mycore.viewer.widgets.thumbnail {
    export interface ThumbnailOverviewThumbnail {
        id: string;
        label: string;
        href: string;
        requestImgdataUrl:(callback:(imgdata:string)=>void)=>void;
    }

}