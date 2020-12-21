package gitlet;

import java.io.File;
import java.io.Serializable;

public class Blob implements Serializable {
    private File file;
    private String userFileName;
    private String content;

    public Blob(File file) {
        this.file = file;
        this.content = "";
    }

    public String getUserFileName() {
        return userFileName;
    }

    public void setUserFileName(String userFileName) {
        this.userFileName = userFileName;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getContent() {
        return content;
//        return Utils.readContentsAsString(file);
    }


    public void setContent(String content) {
        this.content = content;
    }

    public String saveBlob() {
        this.content = Utils.readContentsAsString(this.file);
        setUserFileName(file.getName());
        String id = Utils.sha1(Utils.serialize(this));
        File newBlobFile = Utils.join(Gitlet.BLOB_FOLDER, id);//Write new blob object to Blob folder

        Utils.writeObject(newBlobFile, this);
        return id;
    }

    public static Blob fromBlob(String id) {
        File blobFile = Utils.join(Gitlet.BLOB_FOLDER, id);
        return Utils.readObject(blobFile, Blob.class);
    }

}