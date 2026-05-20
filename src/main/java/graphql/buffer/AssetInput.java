package graphql.buffer;

import org.eclipse.microprofile.graphql.Name;

@Name("AssetInput")
public class AssetInput {
    public ImageAssetInput image;
    public VideoAssetInput video;
    public DocumentAssetInput document;
    public LinkAssetInput link;
}
