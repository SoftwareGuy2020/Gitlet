package gitlet;

import java.io.Serializable;
import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.Date;

/**
 * Repository represents a Gitlet repository.
 *
 * @author Travis & Brian
 */

public class Repository implements Serializable {
	/* String for working directory. */
	static final String WORKING_DIRECTORY = System.getProperty("user.dir");

	/* String for file seperator. */
	static final String FILE_SEP = System.getProperty("file.separator");

	/* String for gitlet directory. */
	static final String GITLET_DIRECTORY = WORKING_DIRECTORY + FILE_SEP + ".gitlet";

	/* String for commit directory. */
	private static final String COMMIT_DIRECTORY = GITLET_DIRECTORY + FILE_SEP + ".commits";

	/* String for version directory. */
	private static final String VERSIONS_DIRECTORY = GITLET_DIRECTORY + FILE_SEP + ".versions";

	/* String for staging area. */
	private static final String STAGING_AREA = GITLET_DIRECTORY + FILE_SEP + ".stage";

	/* UID for serialization. */
	static final long serialVersionUID = 12345678901L;

	/**
	 * HashMap of all branches in repository with string name as key ("master") and
	 * SHA-1 ID of the branch head as values.
	 */
	private HashMap<String, String> branches;

	/**
	 * Hash map of all commits in repository with the SHA-1 ID of commit at key and
	 * pointer to commit object as values.
	 */
	private HashMap<String, Commit> tree;

	private String head;

	// String name ("master") of current branch.
	private String currentBranch;

	// List of string names ("hello.txt") for files to delete.
	private List<String> deletions;

	// Helper functions for head of current branch.

	private String getHeadID(String branchName) {
		return branches.get(branchName);
	}

	private Commit getHeadCommit() {
		return tree.get(head);
	}

	public Repository(HashMap<String, String> branches, HashMap<String, Commit> tree, String head, String currentBranch,
			List<String> deletions) {
		this.branches = branches;
		this.tree = tree;
		this.head = head;
		this.currentBranch = currentBranch;
		this.deletions = deletions;
	}

	/**
	 * Private constructor for creating a Repository with an initial commit.
	 */
	private Repository() {
		this.branches = new HashMap<>();
		this.tree = new HashMap<>();
		this.deletions = new ArrayList<>();
		this.currentBranch = "master";
		Commit initialCommit = new Commit("", "initial commit", new Date(), new HashMap<>());
		tree.put(initialCommit.getId(), initialCommit);
		this.head = initialCommit.getId();
		this.branches.put(this.currentBranch, this.head);
	}

	/**
	 * * Initializes a new gitlet repository.
	 *
	 * @return The new gitlet repository.
	 */
	public static Repository init() {
		File gitlet = new File(GITLET_DIRECTORY);
		gitlet.mkdir();
		File file = new File(STAGING_AREA);
		file.mkdir();
		file = new File(COMMIT_DIRECTORY);
		file.mkdir();
		file = new File(VERSIONS_DIRECTORY);
		file.mkdir();
		return new Repository();
	}

	public void add(String filename) {
		File file = new File(WORKING_DIRECTORY, filename);
		if (!file.exists()) {
			System.out.println("File does not exist.");
			System.exit(0);
		}
		String hash = computeHash(file);
		String original = tree.get(head).getContents().get(filename);

		deletions.remove(filename);
		if (hash.equals(original)) {
			return;
		}
		try {
			File dest = new File(STAGING_AREA, filename);
			Files.copy(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void commit(String message) {
		if (message.isEmpty()) {
			System.out.println("Please enter a commit message.");
			System.exit(0);
		}
		File stage = new File(STAGING_AREA);
		File[] stagedFiles = stage.listFiles();

		if (stagedFiles.length == 0 && deletions.size() == 0) {
			System.out.println("No changes added to the commit.");
			System.exit(0);
		}
		Commit current = tree.get(head);
		HashMap<String, String> contents = new HashMap<>(current.getContents());

		for (String deletion : deletions) {
			contents.remove(deletion);
		}
		for (File stagedFile : stagedFiles) {
			String hash = computeHash(stagedFile);
			contents.put(stagedFile.getName(), hash);

			File dest = new File(VERSIONS_DIRECTORY,
					hash + stagedFile.getName().substring(stagedFile.getName().lastIndexOf('.')));
			try {
				Files.copy(stagedFile.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
				stagedFile.delete();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		current = new Commit(current.getId(), message, new Date(), contents);
		head = current.getId();
		tree.put(head, current);
		branches.put(currentBranch, head);
		deletions.clear();
	}

	public void rm(String filename) {
		File stagedFile = new File(STAGING_AREA, filename);
		boolean deleted = false;
		HashMap<String, String> contents = tree.get(head).getContents();

		if (contents.containsKey(filename)) {
			File file = new File(WORKING_DIRECTORY, filename);
			deleted = true;
			file.delete();
		}
		if (deleted) {
			deletions.add(filename);
		} else if (!stagedFile.delete() && !deleted) {
			System.out.println("No reason to remove the file.");
			System.exit(0);
		}
	}

	public void log() {
		Commit currentCommit = tree.get(head);
		while (currentCommit != null) {
			System.out.println(currentCommit);
			String parentCommitID = currentCommit.getParent();
			currentCommit = tree.get(parentCommitID);
		}
	}

	public void globalLog() {
		for (Commit c : tree.values()) {
			System.out.println(c);
		}
	}

	public void find(String message) {
		boolean isFound = false;
		for (Commit c : tree.values()) {
			if (c.getMessage().equals(message)) {
				isFound = true;
				System.out.println(c.getId());
			}
		}

		if (!isFound) {
			System.out.println("Found no commit with that message.");
		}
	}

	public void status() {
		System.out.println("=== Branches ===");

		branches.keySet().stream().sorted().forEach((x) -> {
			if (x.equals(currentBranch)) {
				System.out.print('*');
			}
			System.out.println(x);
		});
		System.out.println();

		File staged = new File(STAGING_AREA);
		String[] stagedFiles = staged.list();

		System.out.println("=== Staged Files ===");

		if (stagedFiles != null) {
			HashMap<String, Boolean> temp = new HashMap<>();
			for (String file : stagedFiles) {
				temp.put(file, true);
			}
			for (String s : deletions) {
				temp.remove(s);
			}
			Arrays.stream(stagedFiles).sorted().forEach(System.out::println);
			System.out.println();

			System.out.println("=== Removed Files ===");
		}

		if (!deletions.isEmpty()) {
			deletions.stream().sorted().forEach(System.out::println);
		}
		System.out.println("\n=== Modifications Not Staged For Commit ===\n" + "\n=== Untracked Files ===");
	}

	/**
	 * Checkout all files at head of given branch by moving checked out files into
	 * working directory (overwrite if needed). Changes head of current branch to
	 * the given branch. Clears staging area.
	 *
	 * @param branch - the string name of the branch
	 */
	public void checkoutBranch(String branch) {
		String commitID = branches.get(branch);
		if (commitID == null) {
			System.out.println("No such branch exists.");
			System.exit(0);
		}
		if (currentBranch.equals(branch)) {
			System.out.println("No need to checkout the current branch.");
			System.exit(0);
		}

		Commit branchCommit = tree.get(commitID);
		HashMap<String, String> branchCommitContents = branchCommit.getContents();
		Commit currCommit = tree.get(head);
		HashMap<String, String> currContents = currCommit.getContents();

		checkUntrackedConflict(branchCommitContents, currContents);
		File workingDir = new File(WORKING_DIRECTORY);
		for (File file : workingDir.listFiles()) {
			if (!branchCommitContents.containsKey(file.getName()) && currContents.containsKey(file.getName())) {
				file.delete();
			}
		}
		for (Map.Entry<String, String> entry : branchCommitContents.entrySet()) {
			writeBackToWorkingDir(entry.getKey(), entry.getValue());
		}
		head = commitID;
		currentBranch = branch;
	}

	private void checkUntrackedConflict(HashMap<String, String> branchContents, HashMap<String, String> currContents) {
		File workingDir = new File(WORKING_DIRECTORY);
		for (File file : workingDir.listFiles()) {
			if (branchContents.containsKey(file.getName()) && !currContents.containsKey(file.getName())
					&& !computeHash(file).equals(branchContents.get(file.getName()))) {
				System.out.println("There is an untracked file in " + "the way; delete it or add it first.");
				System.exit(0);
			}
		}
	}

	public void checkout(String filename) {
		Commit headCommit = tree.get(head);
		if (!headCommit.getContents().containsKey(filename)) {
			System.out.println("File does not exist in that commit.");
			System.exit(0);
		}
		HashMap<String, String> headFiles = getHeadCommit().getContents();
		String filehash = headFiles.get(filename);
		writeBackToWorkingDir(filename, filehash);
	}

	public void checkout(String id, String filename) {
		if (id.length() < 40) { // id is abbreviated if < 40 digits long
			for (String s : tree.keySet()) { // must check each key to see if it starts with the
				if (s.startsWith(id)) { // abbreviated id.
					id = s;
					break;
				}
			}
		} else if (!tree.containsKey(id)) {
			System.out.println("No commit with that id exists.");
			System.exit(0);
		} else if (!tree.get(id).getContents().containsKey(filename)) {
			System.out.println("File does not exist in that commit.");
			System.exit(0);
		}
		Commit checkoutCommit = tree.get(id);
		String filehash = checkoutCommit.getContents().get(filename);
		writeBackToWorkingDir(filename, filehash);

	}

	public void branch(String name) {
		String branchAdded = branches.put(name, head);
		if (branchAdded != null) {
			System.out.println("A branch with that name already exists.");
		}
	}

	public void rmBranch(String branch) {
		if (branch.equals(currentBranch)) {
			System.out.println("Cannot remove the current branch.");
			System.exit(0);
		}
		String removedBranch = branches.remove(branch);
		if (removedBranch == null) {
			System.out.println("A branch with that name does not exist.");
		}
	}

	public void reset(String id) {
		if (id.length() < 40) { // id is abbreviated if < 40 digits long
			for (String s : tree.keySet()) { // must check each key to see if it starts with the
				if (s.startsWith(id)) { // abbreviated id.
					id = s;
					break;
				}
			}
		}
		if (!tree.containsKey(id)) {
			System.out.println("No commit with that id exists.");
			System.exit(0);
		}
		String temp = "__temp__" + currentBranch + id;
		branches.put(temp, id);
		String savedCurrentBranch = currentBranch;
		checkoutBranch(temp);

		branches.remove(temp);
		currentBranch = savedCurrentBranch;
		branches.put(currentBranch, id);

		File stage = new File(STAGING_AREA);
		for (File stagedFile : stage.listFiles()) {
			stagedFile.delete();
		}
	}

	private void checkBranchErrors(String branch) {
		if (branches.get(branch) == null) {
			System.out.println("A branch with that name does not exist.");
			System.exit(0);
		} else if (currentBranch.equals(branch)) {
			System.out.println("Cannot merge a branch with itself.");
			System.exit(0);
		}
	}

	public void merge(String branch) {
		checkBranchErrors(branch);
		File stagedDir = new File(STAGING_AREA);
		if (!deletions.isEmpty() || stagedDir.list().length > 0) {
			System.out.println("You have uncommitted changes.");
			System.exit(0);
		}
		Commit splitPoint = null, currentCommit = tree.get(head), givenBranchCommit = tree.get(branches.get(branch));
		HashMap<String, String> givenContents = givenBranchCommit.getContents();
		HashMap<String, String> currentContents = currentCommit.getContents();
		checkUntrackedConflict(givenContents, currentContents);
		boolean isFound = false;
		while (!currentCommit.getParent().equals("") && !isFound) {
			while (!givenBranchCommit.getParent().equals("") && !isFound) {
				if (currentCommit.getId().equals(givenBranchCommit.getId())) {
					splitPoint = tree.get(currentCommit.getId());
					isFound = true;
				} else {
					givenBranchCommit = tree.get(givenBranchCommit.getParent());
				}
			}
			currentCommit = tree.get(currentCommit.getParent());
			givenBranchCommit = tree.get(branches.get(branch));
		}
		currentCommit = tree.get(head);
		givenBranchCommit = tree.get(branches.get(branch));

		if (splitPoint.getId().equals(givenBranchCommit.getId())) {
			System.out.println("Given branch is an ancestor of the current branch.");
			return;
		} else if (splitPoint.getId().equals(currentCommit.getId())) {
			branches.put(currentBranch, givenBranchCommit.getId());
			System.out.println("Current branch fast-forwarded.");
			return;
		}
		HashMap<String, String> splitPointContents = splitPoint.getContents();
		String splitFile, givenFile;
		boolean mergeConflict = false;
		for (Map.Entry<String, String> entry : currentContents.entrySet()) {
			splitFile = splitPointContents.get(entry.getKey());
			givenFile = givenContents.get(entry.getKey());
			if (splitFile == null && givenFile == null) {
				continue;
			}
			if (splitFile != null && givenFile == null && splitFile.equals(entry.getValue())) {
				rm(entry.getKey());
			} else if (splitFile != null && splitFile.equals(entry.getValue()) && !splitFile.equals(givenFile)) {
				checkout(givenBranchCommit.getId(), entry.getKey());
				add(entry.getKey());
				givenContents.remove(entry.getKey());
			} else if (splitFile != null && splitFile.equals(givenFile) && !splitFile.equals(entry.getValue())) {
				givenContents.remove(entry.getKey());
			} else if (givenFile != null && !givenFile.equals(entry.getValue())
					|| (splitFile != null && !splitFile.equals(entry.getValue()) && givenFile == null)) {
				mergeConflict = true;
				mergeConcat(entry.getKey(), entry.getValue(), givenFile, givenFile == null);
			}
		}
		for (Map.Entry<String, String> entry : givenContents.entrySet()) {
			splitFile = splitPointContents.get(entry.getKey());
			if (splitFile == null) {
				checkout(givenBranchCommit.getId(), entry.getKey());
				add(entry.getKey());
			} else if (!splitFile.equals(entry.getValue()) && !currentContents.containsKey(entry.getKey())) {
				mergeConflict = true;
				mergeConcat(entry.getKey(), entry.getValue(), "", true);
			}
		}
		if (mergeConflict) {
			System.out.println("Encountered a merge conflict.");
		} else {
			commit("Merged " + currentBranch + " with " + branch + ".");
		}
	}

	private void mergeConcat(String currentFile, String currentHash, String otherHash, boolean otherNull) {

		String ext = currentFile.substring(currentFile.lastIndexOf('.'));
		File file = new File(VERSIONS_DIRECTORY, currentHash + ext);

		String merged = "<<<<<<< HEAD\n";
		merged += new String(Utils.readContents(file)) + "=======\n";

		if (!otherNull) {
			file = new File(VERSIONS_DIRECTORY, otherHash + ext);
			merged += new String(Utils.readContents(file));
		}
		merged += ">>>>>>>\n";
		Utils.writeContents(new File(WORKING_DIRECTORY, currentFile), merged.getBytes());
	}

	public String computeHash(File file) {
		try {
			MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
			FileInputStream in = new FileInputStream(file);

			sha1.update(file.getName().getBytes());
			sha1.update(in.readAllBytes());
			in.close();
			return new BigInteger(1, sha1.digest()).toString(16);

		} catch (NoSuchAlgorithmException | IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private void writeBackToWorkingDir(String filename, String hash) {
		if (hash == null) {
			System.out.println("File does not exist in that commit.");
			System.exit(0);
		}

		File dest = new File(WORKING_DIRECTORY, filename);
		File file = new File(VERSIONS_DIRECTORY, hash + dest.getName().substring(dest.getName().lastIndexOf('.')));

		try {
			Files.copy(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
