package gitlet;

import java.io.File;
import java.util.List;

/**
 * Driver class for Gitlet, the tiny stupid version-control system.
 *
 * @author
 */
public class Main {

    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND> ....
     */
    public static void main(String... args) {
        // FILL THIS IN
//        Stage stage = new Stage();
////        Blob blob = new Blob();
//        Commit commit = new Commit();
//        commit.setMessage("sssss");
//        commit.saveCommit();
//        Commit testCommit = Commit.fromFile("a4c0946b5b91a5d5677d03af3ce6643c0cb1f648");
//        System.out.println(testCommit.getMessage());
//        System.out.println(testCommit.getCommitTime().toString());
//         File file = new File(String.valueOf(Utils.join(".", "wu22g.txt")));
//         Blob blob = new Blob(file);
//         blob.setContent("ddddd");
//         blob.saveBlob();
//        Blob b = Blob.fromBlob("blob_02ff8c4eba3e718175baa291d98fda72067e0202");
//        System.out.println(b.getContent());

//        Stage stage = Stage.fromStage("stage");
//        List<Blob> blobList = stage.getBlobList();
//        for (Blob b : blobList) {
//            System.out.println(b.getContent());
//        }
//       Gitlet gitlet = Gitlet.fromGitlet();
//        System.out.println(gitlet.getHead());
//        String id = "1d5860f30dff35998f3791e744d6cd5e099ca411";
//        String abv = "1d5860f30d";
//        System.out.println(id.startsWith(abv));

        if (args.length == 0) {
            exitWithError("Please enter a command.");
        }

        switch (args[0]) {
            case "init":
                Gitlet gitlet = new Gitlet();
                gitlet.init(args);
                break;
            case "add":
                checkGitRepo();
                gitlet = Gitlet.fromGitlet();
                gitlet.add(args);
                break;
            case "commit":
                checkGitRepo();
                gitlet = Gitlet.fromGitlet();
                gitlet.commit(args);
                break;
            case "checkout":
                checkGitRepo();
                gitlet = Gitlet.fromGitlet();
                gitlet.checkout(args);
                break;
            case "log":
                checkGitRepo();
                gitlet = Gitlet.fromGitlet();
                gitlet.log(args);
                break;
            case "rm":
                checkGitRepo();
                gitlet = Gitlet.fromGitlet();
                gitlet.rm(args);
                break;
            case "global-log":
                checkGitRepo();
                gitlet = Gitlet.fromGitlet();
                gitlet.globalLog(args);
                break;
            case "find":
                checkGitRepo();
                gitlet = Gitlet.fromGitlet();
                gitlet.find(args);
                break;
            case "status":
                checkGitRepo();
                gitlet = Gitlet.fromGitlet();
                gitlet.status(args);
                break;
            case "branch":
                checkGitRepo();
                gitlet = Gitlet.fromGitlet();
                gitlet.branch(args);
                break;
            case "rm-branch":
                checkGitRepo();
                gitlet = Gitlet.fromGitlet();
                gitlet.rmbranch(args);
                break;
            case "reset":
                checkGitRepo();
                gitlet = Gitlet.fromGitlet();
                gitlet.reset(args);
                break;
            case "merge":
                checkGitRepo();
                gitlet = Gitlet.fromGitlet();
                gitlet.merge(args);
                break;
            default:
                exitWithError("No command with that name exists.");
        }
        return;
    }


    /**
     * Prints out MESSAGE and exits with error code 0.
     *
     * @param message message to print
     */
    public static void exitWithError(String message) {
        if (message != null && !message.equals("")) {
            System.out.println(message);
        }
        System.exit(0);
    }

    public static void checkGitRepo() {
        if (!Utils.join(Gitlet.CWD, ".gitlet").exists()) {
            exitWithError("Not in an initialized Gitlet directory.");
        }
    }
}
