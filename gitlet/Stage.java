package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Stage implements Serializable {
    private Map<String, Blob> stagedBlobMap; // key: user file name(staging), value: blob obj
    private Map<String, Blob> removedBlobMap; // key: user file name(removal), value: blob obj


    public Stage() {
        stagedBlobMap = new HashMap<>();
        removedBlobMap = new HashMap<>();
    }

    public void add(String fileName, Blob blob) {
        stagedBlobMap.put(fileName, blob);
    }

    public void unstage(String fileName) {
        stagedBlobMap.remove(fileName);
    }

    public void remove(String fileName, Blob blob) {
        removedBlobMap.put(fileName, blob);
    }

    public void reverseRemove(String fileName) {
        removedBlobMap.remove(fileName);
    }

    public void clearStage() {
        stagedBlobMap.clear();
        removedBlobMap.clear();
    }

    public Map<String, Blob> getStagedBlobMap() {
        return this.stagedBlobMap;
    }

    public Map<String, Blob> getRemovedBlobMap() {
        return this.removedBlobMap;
    }

    public void saveStage() {
        File currentStageFile = Utils.join(".gitlet", "stage");
        Utils.writeObject(currentStageFile, this);

//        if (newStageFile.exists()) {
//            Stage currentStage = fromStage("stage");
//            currentStage.add(fileName, blob);
//            Utils.writeObject(newStageFile, currentStage);
//            for (String file : currentStage.stagedBlobMap.keySet()) {
//                this.add(file, stagedBlobMap.get(file));
//            }
//        } else {
//            this.add(fileName, blob);
//            Utils.writeObject(newStageFile, this);
//        }
    }

    public static Stage fromStage(String name) {
        File stageFile = Utils.join(".gitlet", name);
        return Utils.readObject(stageFile, Stage.class);
    }
}