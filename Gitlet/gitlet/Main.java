package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;

import static gitlet.Repository.*;

/**
 * Driver class for Gitlet, the tiny stupid version-control system.
 * 
 * @author Travis & Brian
 */
public class Main {

	private static final String REPO = "repo.bin";
	private static final String INCORRECT_OPERANDS_ERROR = "Incorrect operands.";

	private static Repository repository;
	private static int commandLength;
	private static String[] argList;

	/**
	 * Helper method that checks that the length of the argument list is the correct
	 * length. This method is to be called after verifying the command is a valid
	 * command. If the length is not correct, this method prints an error message
	 * defined under the INCORRECT_OPERANDS_ERROR constant.
	 * 
	 * @param len the correct length of args to check.
	 * @return true if args length equals len. false otherwise.
	 */
	private static boolean checkOperands(int len) {
		if (commandLength == len) {
			return true;
		} else {
			System.out.println(INCORRECT_OPERANDS_ERROR);
			return false;
		}
	}

	private static void init() {
		if (repository != null) {
			System.out.println("A gitlet version-control system " + "already exists in the current directory.");
		} else if (checkOperands(1)) {
			repository = Repository.init();
			save();
		}
	}

	private static void add() {
		if (checkOperands(2)) {
			repository.add(argList[1]);
			save();
		}
	}

	private static void commit() {
		if (checkOperands(2)) {
			repository.commit(argList[1]);
			save();
		}
	}

	private static void rm() {
		if (checkOperands(2)) {
			repository.rm(argList[1]);
			save();
		}
	}

	private static void log() {
		if (checkOperands(1)) {
			repository.log();
		}
	}

	private static void globalLog() {
		if (checkOperands(1)) {
			repository.globalLog();
		}
	}

	private static void find() {
		if (checkOperands(2)) {
			repository.find(argList[1]);
		}
	}

	private static void status() {
		if (checkOperands(1)) {
			repository.status();
		}
	}

	private static void checkoutBranch(String branch) {
		repository.checkoutBranch(branch);
		save();
	}

	private static void checkout(String filename) {
		repository.checkout(filename);
		save();
	}

	private static void checkout(String id, String filename) {
		repository.checkout(id, filename);
		save();
	}

	private static void branch() {
		if (checkOperands(2)) {
			repository.branch(argList[1]);
			save();
		}
	}

	private static void rmBranch() {
		if (checkOperands(2)) {
			repository.rmBranch(argList[1]);
			save();
		}
	}

	private static void reset() {
		if (checkOperands(2)) {
			repository.reset(argList[1]);
			save();
		}
	}

	private static void merge() {
		if (checkOperands(2)) {
			repository.merge(argList[1]);
			save();
		}
	}

	/**
	 * Saves the repository to file
	 */
	private static void save() {
		File file = new File(GITLET_DIRECTORY, REPO);
		try {
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file));
			out.writeObject(repository);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 * Usage: java gitlet.Main ARGS, where ARGS contains <COMMAND> <OPERAND> ....
	 */
	public static void main(String... args) {
		commandLength = args.length;
		argList = args;
		if (commandLength == 0) {
			System.out.println("Please enter a command.");
			System.exit(0);
		}
		File gitletDir = new File(GITLET_DIRECTORY);
		if (gitletDir.exists()) {
			File inFile = new File(gitletDir, REPO);
			try {
				ObjectInputStream in = new ObjectInputStream(new FileInputStream(inFile));
				repository = (Repository) in.readObject();
				in.close();
			} catch (IOException | ClassNotFoundException e) {
				// do nothing
			}
		}
		String command = args[0];

		if (command.equals("init")) {
			init();
		} else if (repository == null) {
			System.out.println("Not in an initialized gitlet directory.");
			System.exit(0);
		} else if (command.equals("status")) {
			status();
		} else if (command.equals("global-log")) {
			globalLog();
		} else if (command.equals("log")) {
			log();
		} else if (command.equals("add")) {
			add();
		} else if (command.equals("commit")) {
			commit();
		} else if (command.equals("find")) {
			find();
		} else if (command.equals("checkout")) {
			if (commandLength == 2) {
				checkoutBranch(args[1]);
			} else if (commandLength == 3 && argList[1].equals("--")) {
				checkout(args[2]);
			} else if (commandLength == 4 && argList[2].equals("--")) {
				checkout(args[1], args[3]);
			} else {
				System.out.println(INCORRECT_OPERANDS_ERROR);
			}
		} else if (command.equals("branch")) {
			branch();
		} else if (command.equals("rm-branch")) {
			rmBranch();
		} else if (command.equals("rm")) {
			rm();
		} else if (command.equals("reset")) {
			reset();
		} else if (command.equals("merge")) {
			merge();
		} else {
			System.out.println("No command with that name exists.");
		}
	}
}
