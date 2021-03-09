package gitlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/**
 * Commit represents a single commit in a Gitlet repository.
 */
public class Commit implements Serializable {
	static final long serialVersionUID = 12345678900L;

	private final String id;
	private final String parent;
	private final String message;
	private final Date commitDate;
	private final HashMap<String, String> contents;

	public Commit(String id, String parent, String message, Date commitDate, HashMap<String, String> contents) {
		this.id = id;
		this.parent = parent;
		this.message = message;
		this.commitDate = commitDate;
		this.contents = contents;
	}

	public Commit(String parent, String message, Date commitDate, HashMap<String, String> contents) {
		this.parent = parent;
		this.message = message;
		this.commitDate = commitDate;
		this.contents = contents;
		this.id = computeHash();
	}

	public String getId() {
		return id;
	}

	public String getParent() {
		return parent;
	}

	public String getMessage() {
		return message;
	}

	public Date getCommitDate() {
		return commitDate;
	}

	public HashMap<String, String> getContents() {
		return new HashMap<>(contents);
	}

	@Override
	public String toString() {
		return "===\n" + "Commit " + id + "\n" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(commitDate) + '\n'
				+ message + '\n';
	}

	private String computeHash() {
		try {
			MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
			sha1.update(this.parent.getBytes());
			sha1.update(this.message.getBytes());
			sha1.update(this.commitDate.toString().getBytes());

			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			ObjectOutputStream objectStream = new ObjectOutputStream(stream);
			objectStream.writeObject(this.contents);
			objectStream.close();
			sha1.update(stream.toByteArray());

			return new BigInteger(1, sha1.digest()).toString(16);
		} catch (NoSuchAlgorithmException | IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}
