package gitlet;

import javax.swing.*;
import java.io.File;
import java.io.Serializable;
import java.lang.management.BufferPoolMXBean;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.BlockingQueue;

public class Gitlet implements Serializable {
    private Stage stage;
    private String head;
    private String initialCommitId;
    private String currentBranchName;
    static final File CWD = new File(".");
    static final File GITLET_FOLDER = Utils.join(CWD, ".gitlet");
    static final File BLOB_FOLDER = Utils.join(GITLET_FOLDER, "Blob");
    static final File COMMIT_FOLDER = Utils.join(GITLET_FOLDER, "Commit");

    //    private Map<String, Blob> blobMap; // key: user file name, value: Blob object
    private Map<String, Commit> branchMap; // key: branch name, value: Commit object
    private Map<String, TreeSet<String>> branchCommitsMap;
    private Map<Commit, String> commitMap; // key: commit, value: commit id
    private Map<String, Commit> commitIDMap; // key: commit id, value commit object

    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public String getHead() {
        return head;
    }

    public void setHead(String head) {
        this.head = head;
    }

    public String getInitialCommitId() {
        return initialCommitId;
    }

    public void setInitialCommitId(String initialCommitId) {
        this.initialCommitId = initialCommitId;
    }

    public Gitlet() {
//        blobMap = new HashMap<>();
        stage = new Stage();
        branchMap = new HashMap<>();
        commitMap = new HashMap<>();
        commitIDMap = new HashMap<>();
        branchCommitsMap = new TreeMap<>();
    }

    public void init(String[] args) {
        validateNumArgs(args, 1);
        if (GITLET_FOLDER.exists()) {
            exitWithError("A Gitlet version-control system already exists in the current directory.");
//            throw new GitletException("A Gitlet version-control system already exists in the current directory.");
        }
        GITLET_FOLDER.mkdir();
        BLOB_FOLDER.mkdir();
        COMMIT_FOLDER.mkdir();
        Commit dummyCommit = new Commit();
        dummyCommit.setMessage("initial commit");
        dummyCommit.setCommitTime("Wed Dec 31 17:00:00 1969 -0700");
        head = dummyCommit.saveCommit();
        commitMap.put(dummyCommit, head);
        commitIDMap.put(head, dummyCommit);
        initialCommitId = head;
        branchMap.put("master", dummyCommit);
        currentBranchName = "master";
        TreeSet<String> set = new TreeSet<>();
        set.add(head);
        branchCommitsMap.put(currentBranchName, set);

        saveGitlet();
    }

    public void add(String[] args) {
        validateNumArgs(args, 2);
        String fileName = args[1];
        File fileToStage = Utils.join(CWD, fileName);
        if (!fileToStage.exists()) {
            exitWithError("File does not exist.");
//            throw new GitletException("File does not exist.");
        }
        Commit currentCommit = commitIDMap.get(head);
        String originalContent = "";
        if (currentCommit.getRefToBlob().containsKey(fileName)) { // if the current head commit already has a version of that file, set its content as original content
            originalContent = currentCommit.getRefToBlob().get(fileName).getContent();
        }
        String newContent = Utils.readContentsAsString(fileToStage); // reads the content of the file user wants to stage
        if (originalContent.equals(newContent)) { // If the current working version of the file is identical to the version in the current commit
            if (stage.getStagedBlobMap().containsKey(fileName)) {
                stage.unstage(fileName);
            }
            if (stage.getRemovedBlobMap().containsKey(fileName)) {
                stage.reverseRemove(fileName);
            }
        } else {
            Blob blobToAdd = new Blob(fileToStage);
            blobToAdd.saveBlob();
            stage.add(fileName, blobToAdd);
        }
        stage.saveStage();
        saveGitlet();
    }

    /**
     * A commit tracks all the files tracked by its parent, and add new or update existing files that have been staged for commit.
     * The bottom line: By default a commit is the same as its parent. Files staged for addition and removal are the updates to the commit.
     */
    public void commit(String[] args) {
        validateNumArgs(args, 2);
        if (args[1].isBlank()) {
            exitWithError("Please enter a commit message.");
//            throw new GitletException("Please enter a commit message.");
        }
        if (stage.getStagedBlobMap().isEmpty() && stage.getRemovedBlobMap().isEmpty()) {
            exitWithError("No changes added to the commit.");
//            throw new GitletException("No changes added to the commit.");
        }
        Commit newCommit = new Commit();
        newCommit.setMessage(args[1]);
        newCommit.setCommitTime(new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z").format(new Date()));
        Map<String, Blob> blobsFromParent = Commit.fromCommit(head).getRefToBlob();//todo: Parental commit's blobMap

        if (blobsFromParent != null) {
            blobsFromParent.putAll(stage.getStagedBlobMap()); // update/add to blobs from parent commit with newly staged blobs
            blobsFromParent.keySet().removeAll(stage.getRemovedBlobMap().keySet()); // remove the blobs that have been staged for removal, so the new commit doesn't track it anymore
            Map<String, Blob> mergedBlobMap = blobsFromParent;
            newCommit.setRefToBlob(mergedBlobMap);
        } else { // parent commit is dummy commit
            newCommit.setRefToBlob(stage.getStagedBlobMap());
        }
        newCommit.setRefToParent(head);
        head = newCommit.saveCommit(); // update the head to be the new commit
        commitMap.put(newCommit, head);
        commitIDMap.put(head, newCommit);
        branchMap.put(currentBranchName, newCommit);
        TreeSet<String> set = branchCommitsMap.get(currentBranchName);
//        if (set == null) {
//            set = new TreeSet<>();
//        }
        set.add(head);
        branchCommitsMap.put(currentBranchName, set);
        stage.clearStage();
        stage.saveStage();
        saveGitlet();
    }

    public void log(String[] args) {
        validateNumArgs(args, 1);
        String ptr = head;
        while (ptr != null) {
            Commit curCommit = Commit.fromCommit(ptr);
            System.out.println("===");
            System.out.println("commit " + ptr);
            System.out.println("Date: " + curCommit.getCommitTime());
            System.out.println(curCommit.getMessage() + "\n");
//            System.out.println(ptr);
//            System.out.println(curCommit.getMessage() + "\n");
            ptr = curCommit.getRefToParent();
        }
    }

    public void globalLog(String[] args) {
        validateNumArgs(args, 1);

        for (File file : COMMIT_FOLDER.listFiles()) {
            String ptr = file.getName();
            if (file.isFile()) {
                Commit curCommit = Commit.fromCommit(ptr);
                System.out.println("===");
                System.out.println("commit " + ptr);
                System.out.println("Date: " + curCommit.getCommitTime());
                System.out.println(curCommit.getMessage() + "\n");
            }
        }
    }
//        for (File file : CWD.listFiles()) {// Not linear relative to the # of commits at all, but a single map relation can make it Theta(1) :>>.
//            String ptr = file.getName();
//            if (file.isFile() && ptr.contains("commit_")) {// Make sure file is File and it is a commit file
//                Commit curCommit = Commit.fromCommit(ptr);
//                System.out.println("===");
//                System.out.println("commit " + ptr.split("_")[1]);
//                System.out.println("Date: " + curCommit.getCommitTime());
//                System.out.println(curCommit.getMessage() + "\n");
//            }
//        }

    public void rm(String[] args) {
        validateNumArgs(args, 2);
        String fileName = args[1];
        Commit currentCommit = commitIDMap.get(head);
        boolean inStage = stage.getStagedBlobMap().containsKey(fileName);
        boolean inCommit = currentCommit.getRefToBlob().containsKey(fileName);
        if (!inStage && !inCommit) {
            exitWithError("No reason to remove the file.");
//            throw new GitletException("No reason to remove the file.");

        } else { // If the file is both staged for addition and tracked in the current commit, a single call to rm should complete all intended behavior
            if (inStage) { // if in stage - unstage it: delete it from stageBlobMap
                stage.unstage(fileName);
            }
            if (inCommit) { // if in current commit - stage it for removal and remove the file from the working directory
                Blob blobToRemove = currentCommit.getRefToBlob().get(fileName);
                stage.remove(fileName, blobToRemove);
                File fileToRemove = Utils.join(CWD, fileName);
                Utils.restrictedDelete(fileToRemove);
            }
        }
        stage.saveStage();
        saveGitlet();
    }

    public void find(String[] args) {//todo: Untested after completion
        validateNumArgs(args, 2);
        int count = 0;// To tell whether this message belongs to any commits in all the files.

        for (File file : COMMIT_FOLDER.listFiles()) {
            String ptr = file.getName();
            Commit curCommit = commitIDMap.get(ptr);
            if (file.isFile() && curCommit.getMessage().equals(args[1])) {
                count++;
                System.out.println(file.getName());
            }
        }
        if (count == 0) {
            exitWithError("Found no commit with that message");
//            throw new GitletException("Found no commit with that message");
        }
    }

//        for (File file : CWD.listFiles()) {// Not linear relative to the # of commits at all, but a single map relation can make it Theta(1) :>>.
//            String ptr = file.getName();// if We add all the map relations(commit,blob...) when we were adding them, we don't even need to go through the entire CWD to find which file is commit or blob.
//            if (file.isFile() && ptr.contains("commit_")) {// Make sure file is File and it is a commit file
//                Commit curCommit = Commit.fromCommit(ptr);
//                if (curCommit.getMessage().equals(args[1])) {
//                    count++;
//                    System.out.println(file.getName().split("_")[1]);//print ID of commits by regex.
//                }
//            }
//        }
//        if (count == 0) {
//            throw new GitletException("Found no commit with that message");
//        }

    public void status(String[] args) {
        validateNumArgs(args, 1);
        System.out.println("=== Branches ===");
        Set<String> branches = branchMap.keySet();
        for (String branch : branches) {
            if (branch.equals(currentBranchName)) {
                if (!currentBranchName.equals("master")) {
                    branch += "-branch";
                }
                branch = "*" + branch;

            }
            System.out.println(branch);
        }
        System.out.println("\n=== Staged Files ===");
        Set<String> stagedFiles = stage.getStagedBlobMap().keySet();
        for (String fileName : stagedFiles) {
            System.out.println(fileName);
        }
        System.out.println("\n=== Removed Files ===");
        Set<String> removedFiles = stage.getRemovedBlobMap().keySet();
        for (String removedFile : removedFiles) {
            System.out.println(removedFile);
        }
        System.out.println("\n=== Modifications Not Staged For Commit ===\n\n=== Untracked Files ===\n"); //todo: EC opportunity for the last 2 sections
    }

    public void checkout(String[] args) {
        Boolean usage1 = args.length == 3 && args[1].equals("--"); // usage #1: java gitlet.Main checkout -- [file name]
        Boolean usage2 = args.length == 4 && args[2].equals("--"); // usage #2: java gitlet.Main checkout [commit id] -- [file name]
        Boolean usage3 = args.length == 2; // usage #3: java gitlet.Main checkout [branch name]
        if ((!usage1) && (!usage2) && (!usage3)) {
            exitWithError("Incorrect operands.");
//            throw new GitletException("Incorrect operands.");
        }
        if (usage1) {
            String fileName = args[2];
            Commit commitToCheckOut = commitIDMap.get(head);
            checkoutFile(commitToCheckOut, fileName);
        }
        if (usage2) {
            String commitID = convertCommitID(args[1]);
            if (commitID == null) {
                exitWithError("No commit with that id exists.");
//                throw new GitletException("No commit with that id exists.");
            }
            String fileName = args[3];
            Commit commitToCheckOut = commitIDMap.get(commitID);
            checkoutFile(commitToCheckOut, fileName);
        }
        if (usage3) {
            String branchToCheckOut = args[1];
            if (!branchMap.containsKey(branchToCheckOut)) { // if no branch with that name exists
                exitWithError("No such branch exists.");
//                throw new GitletException("No such branch exists.");
            }
            if (branchToCheckOut.equals(currentBranchName)) { // if that branch is the current branch
                exitWithError("No need to checkout the current branch.");
//                throw new GitletException("No need to checkout the current branch.");
            }
            Commit currentCommit = commitIDMap.get(head);
            Commit commitToCheckOut = branchMap.get(branchToCheckOut);
            checkoutCommit(currentCommit, commitToCheckOut);//todo: Perform this check before doing anything else;
            head = commitMap.get(commitToCheckOut);// change head pointer to current commit
            currentBranchName = branchToCheckOut; // set the current branch to be the checked out branch
            saveGitlet();
        }
    }

    /**
     * Complete the required behaviors when a specific file is checked out. Used in the first two use cases of checkout.
     *
     * @param commitToCheckOut
     * @param fileName
     */
    private void checkoutFile(Commit commitToCheckOut, String fileName) {
        if (!commitToCheckOut.getRefToBlob().containsKey(fileName)) {
            exitWithError("File does not exist in that commit");
//            throw new GitletException("File does not exist in that commit");
        }
        Blob blobToCheckOut = commitToCheckOut.getRefToBlob().get(fileName);
        File fileToReplace = Utils.join(CWD, fileName);
        Utils.writeContents(fileToReplace, blobToCheckOut.getContent());
    }

    /**
     * Complete the required behaviors when an arbitrary commit is checked out. Used in check out and reset.
     *
     * @param currentCommit
     * @param commitToCheckOut
     */
    public void checkoutCommit(Commit currentCommit, Commit commitToCheckOut) {
        Set<String> filesInCurrentCommit = currentCommit.getRefToBlob().keySet();
        Set<String> filestoCheckOut = commitToCheckOut.getRefToBlob().keySet();
        for (String fileName : filestoCheckOut) { // take files in head commit of checked-out branch and put them in CWD, overwriting the versions of existing files
            if (!filesInCurrentCommit.contains(fileName) && Utils.join(CWD, fileName).exists()) { // if a working file is untracked in the current branch and would be overwritten by the checkout
                exitWithError("There is an untracked file in the way; delete it, or add and commit it first.");
//                throw new GitletException("There is an untracked file in the way; delete it, or add and commit it first.");
            }
            File fileToReplace = Utils.join(CWD, fileName);
            Blob blobToCheckOut = commitToCheckOut.getRefToBlob().get(fileName);
            String content = blobToCheckOut.getContent();
            Utils.writeContents(fileToReplace, content);
        }
        for (String fileName : filesInCurrentCommit) { // Any files that are tracked in the current branch but are not present in the checked-out branch are deleted.
            if (!filestoCheckOut.contains(fileName)) {
                Utils.restrictedDelete(Utils.join(CWD, fileName));
            }
        }
        stage.clearStage();
        stage.saveStage();
    }

    public void branch(String[] args) {
        validateNumArgs(args, 2);
        String newBranchName = args[1];
        if (branchMap.containsKey(newBranchName)) {
            exitWithError("A branch with that name already exists.");
//            throw new GitletException("A branch with that name already exists.");
        }
        Commit currentCommit = branchMap.get(currentBranchName);
        branchMap.put(newBranchName, currentCommit);
        TreeSet<String> set = new TreeSet<>();
        set.add(head);
        branchCommitsMap.put(newBranchName, set);
        saveGitlet();
    }

    public void rmbranch(String[] args) {
        validateNumArgs(args, 2);
        String branchToRemove = args[1];
        if (!branchMap.containsKey(branchToRemove)) {
            exitWithError("A branch with that name does not exist.");
//            throw new GitletException("A branch with that name does not exist.");
        }
        if (currentBranchName.equals(branchToRemove)) {
            exitWithError("Cannot remove the current branch.");
//            throw new GitletException("Cannot remove the current branch.");
        }
        branchMap.remove(branchToRemove);
        saveGitlet();
    }

    public void reset(String[] args) {
        validateNumArgs(args, 2);
        String commitID = convertCommitID(args[1]);
        if (commitID == null) {
            exitWithError("No commit with that id exists.");
//            throw new GitletException("No commit with that id exists.");
        }
        Commit currentCommit = branchMap.get(currentBranchName);
        Commit commitToReset = commitIDMap.get(commitID);
        checkoutCommit(currentCommit, commitToReset);
        head = commitID;
        String branchName = null;
//        for (String branch : branchMap.keySet()) {
//            Commit commit = branchMap.get(branch);
//            if (commitMap.get(commit).equals(commitID)) {
//                branchName = branch;
//            }
//        }
        Set<String> branchNames = branchCommitsMap.keySet();
        for (String branch : branchNames) {
            if (branchCommitsMap.get(branch).contains(commitID)) {
                currentBranchName = branch;
            }
        }
//        currentBranchName = branchName;

        saveGitlet();
    }

    public void merge(String[] args) {
        validateNumArgs(args, 2);
        String givenBranchName = args[1];
        if (!stage.getRemovedBlobMap().isEmpty() || !stage.getStagedBlobMap().isEmpty()) {//Failure case 1
            exitWithError("You have uncommitted changes.");
        }
        if (!branchMap.containsKey(givenBranchName)) {// Failure case 2(exit or not?)
            exitWithError("A branch with that name does not exist.");
        }
        if (currentBranchName.equals(givenBranchName)) {// Failure case 3(exit or not?)
            exitWithError("Cannot merge a branch with itself.");
        }
        /*If merge would generate an error because the commit that it does has
        no changes in it, just let the normal commit error message for this go through.*/

        /*if(branchMap.get(branchName).getRefToBlob() == branchMap.get(currentBranchName).getRefToBlob()){//We can let the normal commit error go through
        }*/

        //If an untracked file in the current commit would be overwritten or deleted by the merge, print There is an untracked file in the way;
        // delete it, or add and commit it first. and exit
        Commit currentCommit = branchMap.get(currentBranchName);
        Commit givenBranchCommit = branchMap.get(givenBranchName);
        Set<String> filesInCurrentCommit = currentCommit.getRefToBlob().keySet();
        Set<String> filesInGivenCommit = givenBranchCommit.getRefToBlob().keySet();
        for (String fileName : filesInGivenCommit) { // take files in head commit of checked-out branch and put them in CWD, overwriting the versions of existing files
            if (!filesInCurrentCommit.contains(fileName) && Utils.join(CWD, fileName).exists()) { // if a working file is untracked in the current branch and would be overwritten by the checkout
                exitWithError("There is an untracked file in the way; delete it, or add and commit it first.");
            }
        }
        Commit splitPoint = null;
        NavigableSet<String> givenBranchCommits = branchCommitsMap.get(givenBranchName).descendingSet();
        NavigableSet<String> currentBranchCommits = branchCommitsMap.get(currentBranchName).descendingSet();
        for (String commit : givenBranchCommits) {
            if (currentBranchCommits.contains(commit)) {
                splitPoint = commitIDMap.get(commit);
                break;
            }
        }
        Set<String> filesInSplitCommit = splitPoint.getRefToBlob().keySet();

//        ArrayList splitPointIDArray = new ArrayList();
//        String currCommitID = commitMap.get(currentCommit);
//        while (currCommitID != null) {
//            String givBranchCommitID = commitMap.get(givenBranchCommit);
//            while (givBranchCommitID != null) {
//                if (currCommitID.equals(givBranchCommitID)) {
//                    splitPointIDArray.add(currCommitID);//And the 1st element in this Array should be the splitPoint
//                }
//                givBranchCommitID = commitIDMap.get(givBranchCommitID).getRefToParent();//Move to its parent
//            }
//            currCommitID = commitIDMap.get(currCommitID).getRefToParent();//Move to its parent
//        }
//        splitPoint = commitIDMap.get(splitPointIDArray.get(0));//And the 1st element in this Array should be the splitPoint

        /**If the split point is the same commit as the given branch, then we do nothing; the merge is complete
         and the operation ends with the message Given branch is an ancestor of the current branch.**/
        if (splitPoint == givenBranchCommit) {
            exitWithError("Given branch is an ancestor of the current branch.");
        }
        /**If the split point is the current branch, then the effect is to check out the given branch, and the operation
         * ends after printing the message Current branch fast-forwarded.**/
        if (splitPoint == currentCommit) {
            checkout(new String[]{"checkout", givenBranchName});
            exitWithError("Current branch fast-forwarded.");
        }
        boolean inConflict = false;
        if (filesInCurrentCommit != null) {
            for (String fileName : filesInSplitCommit) {
                if (filesInGivenCommit.contains(fileName) && filesInCurrentCommit.contains(fileName)) {
                    Blob splitBlob = splitPoint.getRefToBlob().get(fileName);
                    Blob givenBlob = givenBranchCommit.getRefToBlob().get(fileName);
                    Blob currentBlob = currentCommit.getRefToBlob().get(fileName);
                    if ((!splitBlob.equals(givenBlob)) && splitBlob.equals(currentBlob)) {
                        File fileToReplace = Utils.join(CWD, fileName);
                        Utils.writeContents(fileToReplace, givenBlob.getContent());
                        stage.add(fileName, givenBlob);
                        stage.saveStage();
                    } else if ((!splitBlob.equals(givenBlob)) && (!splitBlob.equals(currentBlob)) && (!givenBlob.equals(currentBlob))) {
                        mergeConflict(fileName, currentBlob, givenBlob);
                        inConflict = true;
                    }
                } else if ((!filesInGivenCommit.contains(fileName)) && filesInCurrentCommit.contains(fileName)) { //Any files present at the split point, unmodified in the current branch, and absent in the given branch should be removed (and untracked).
                    Blob splitBlob = splitPoint.getRefToBlob().get(fileName);
                    Blob currentBlob = currentCommit.getRefToBlob().get(fileName);
                    if (splitBlob.equals(currentBlob)) {
                        rm(new String[]{"rm", fileName});
                    } else {
                        Blob givenBlob = new Blob(Utils.join(CWD, fileName));
                        mergeConflict(fileName, currentBlob, givenBlob);
                        inConflict = true;
                    }
                } else if (filesInGivenCommit.contains(fileName) && (!filesInCurrentCommit.contains(fileName))) {
                    Blob splitBlob = splitPoint.getRefToBlob().get(fileName);
                    Blob givenBlob = givenBranchCommit.getRefToBlob().get(fileName);
                    if (!splitBlob.equals(givenBlob)) {
                        Blob currentBlob = new Blob(Utils.join(CWD, fileName));
                        mergeConflict(fileName, currentBlob, givenBlob);
                        inConflict = true;
                    }
                }
            }
        } else if (filesInCurrentCommit != null) {

            for (String fileName : filesInGivenCommit) {
                if ((!filesInSplitCommit.contains(fileName)) && (!filesInCurrentCommit.contains(fileName))) {
                    Blob givenBlob = givenBranchCommit.getRefToBlob().get(fileName);
                    File fileToReplace = Utils.join(CWD, fileName);
                    Utils.writeContents(fileToReplace, givenBlob.getContent());
                    stage.add(fileName, givenBlob);
                    stage.saveStage();
                } else if ((!filesInSplitCommit.contains(fileName)) && filesInCurrentCommit.contains(fileName)) {
                    Blob givenBlob = givenBranchCommit.getRefToBlob().get(fileName);
                    Blob currentBlob = currentCommit.getRefToBlob().get(fileName);
                    if (!givenBlob.equals(currentBlob)) {
                        mergeConflict(fileName, currentBlob, givenBlob);
                        inConflict = true;
                    }
                }
            }
        }
        if (!inConflict) {
            Commit newCommit = new Commit();
            newCommit.setMessage("Merged " + givenBranchName + " into " + currentBranchName + ".");
            newCommit.setCommitTime(new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z").format(new Date()));
            Map<String, Blob> blobsFromParent = Commit.fromCommit(head).getRefToBlob();
            if (blobsFromParent != null) {
                blobsFromParent.putAll(stage.getStagedBlobMap()); // update/add to blobs from parent commit with newly staged blobs
                blobsFromParent.keySet().removeAll(stage.getRemovedBlobMap().keySet()); // remove the blobs that have been staged for removal, so the new commit doesn't track it anymore
                Map<String, Blob> mergedBlobMap = blobsFromParent;
                newCommit.setRefToBlob(mergedBlobMap);
            } else { // parent commit is dummy commit
                newCommit.setRefToBlob(stage.getStagedBlobMap());
            }
            newCommit.setRefToParent(commitMap.get(givenBranchCommit));
            head = newCommit.saveCommit(); // update the head to be the new commit
            commitMap.put(newCommit, head);
            commitIDMap.put(head, newCommit);
            branchMap.put(currentBranchName, newCommit);
            branchMap.put(givenBranchName, newCommit);
            TreeSet<String> set = branchCommitsMap.get(currentBranchName);
            set.add(head);
            branchCommitsMap.put(currentBranchName, set);
            TreeSet<String> set2 = branchCommitsMap.get(givenBranchName);
            set.add(head);
            branchCommitsMap.put(givenBranchName, set);
            stage.clearStage();
            stage.saveStage();
        } else {
            exitWithError("Encountered a merge conflict.");
        }
        saveGitlet();
    }

    public void mergeConflict(String fileName, Blob currentBlob, Blob givenBlob) {
        File conflictedFile = Utils.join(CWD, fileName);
        String newContent = "<<<<<<< HEAD" + "\n" + currentBlob.getContent() + "\n" + "=======" + "\n" + givenBlob.getContent();
        Utils.writeContents(conflictedFile, newContent);
    }

    /**
     * Find the commit id that matches the abbreviated id provided by user. Used in checkout and reset.
     */
    public String convertCommitID(String abbrevID) {
        for (String id : commitMap.values()) {
            if (id.startsWith(abbrevID)) {
                return id;
            }
        }
        return null;
    }

    public void saveGitlet() {
        File newGitletFile = Utils.join(".gitlet", "gitlet");
        Utils.writeObject(newGitletFile, this);
    }

    public static Gitlet fromGitlet() {
        File gitletFile = Utils.join(".gitlet", "gitlet");
        return Utils.readObject(gitletFile, Gitlet.class);
    }

    /**
     * Checks the number of arguments versus the expected number,
     * throws a GitletException if they do not match.
     *
     * @param args Argument array from command line
     * @param n    Number of expected arguments
     */
    public static void validateNumArgs(String[] args, int n) {
        if (args.length != n) {
            exitWithError("Incorrect operands.");
//            throw new GitletException("Incorrect operands.");
        }
    }

    public static void exitWithError(String message) {
        if (message != null && !message.equals("")) {
            System.out.println(message);
        }
        System.exit(0);
    }
}
