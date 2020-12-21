package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Commit implements Serializable {
    private String commitTime;
    private String message;
    private String refToParent;
    private Map<String, Blob> refToBlobMap; // key: user file name, value: blob object

    public Commit() {
        refToBlobMap = new HashMap<>();
    }
    public String getMessage() {
        return message;
    }


    public void setMessage(String message) {
        this.message = message;
    }

    public String getRefToParent() {
        return refToParent;
    }

    public void setRefToParent(String refToParent) {
        this.refToParent = refToParent;
    }

    public Map<String, Blob> getRefToBlob() {
        return refToBlobMap;
    }

    public void setRefToBlob(Map<String, Blob> refToBlobMap) {
        this.refToBlobMap = refToBlobMap;
    }

    public String getCommitTime() {
        return commitTime;
    }

    public void setCommitTime(String commitTime) {
        this.commitTime = commitTime;
    }

    public String saveCommit() {
        String id = Utils.sha1(Utils.serialize(this));//Commit's name
        File newCommitFile = Utils.join(Gitlet.COMMIT_FOLDER, id);//Write new commit object to Commit folder
        Utils.writeObject(newCommitFile, this);
        return id;
    }

    public static Commit fromCommit(String id) {
        File commitFile = Utils.join(Gitlet.COMMIT_FOLDER, id);
        return Utils.readObject(commitFile, Commit.class);
    }
}