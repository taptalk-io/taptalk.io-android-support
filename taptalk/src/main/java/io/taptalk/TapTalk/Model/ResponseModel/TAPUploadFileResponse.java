package io.taptalk.TapTalk.Model.ResponseModel;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TAPUploadFileResponse {
    @JsonProperty("id") private String id;
    @JsonProperty("mediaType") private String mediaType;
    @JsonProperty("size") private long size;
    @JsonProperty("width") private int width;
    @JsonProperty("height") private int height;
    @JsonProperty("caption") private String caption;

    public TAPUploadFileResponse(String id, String mediaType, long size, int width, int height, String caption) {
        this.id = id;
        this.mediaType = mediaType;
        this.size = size;
        this.width = width;
        this.height = height;
        this.caption = caption;
    }

    public TAPUploadFileResponse() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }
}
