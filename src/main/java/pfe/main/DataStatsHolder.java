package pfe.main;

/**
 * 
 * @author Yassine Badache
 * 
 *         The StatsDataHolder class contains all the datas used to measure
 *         performance of the software, based on which information we need
 * 
 *         At the moment, we need the following, listed as parameters:
 *
 */
public class DataStatsHolder {
	/** Number of files that cannot be treated with Gumtree Spoon */
	private int nbFileErrors;

	/** Number of commits */
	private int nbCommits;

	/** Number of CtAssignment contained in all commits */
	private int nbAssignment;

	/** Number of CtLocalVar contained in all commits */
	private int nbLocalVar;

	/** Number of CtReturn contained in all commits */
	private int nbReturn;

	/** Number of CtFieldRead contained in all commits */
	private int nbFieldWrite;

	/** Number of commits that contain an error file diff */
	private int nbCommitsWithError;

	private String errorString;

	/**
	 * Basic constructor
	 */
	public DataStatsHolder() {
		nbFileErrors = 0;
		nbCommits = 0;
		nbCommitsWithError = 0;

		nbFieldWrite = 0;
		nbAssignment = 0;
		nbLocalVar = 0;
		nbReturn = 0;

		errorString = "";
	}

	public void increment(String variableToIncrement) {
		errorString = "";

		switch (variableToIncrement) {
		case ("commit"):
			nbCommits++;
		break;

		case ("file_error"):
			nbFileErrors++;
		break;

		case ("Assignment"):
			nbAssignment++;
		break;

		case ("LocalVariable"):
			nbLocalVar++;
		break;

		case ("Return"):
			nbReturn++;
		break;

		case ("FieldWrite"):
			nbFieldWrite++;
		break;

		case ("commit_error"):
			nbCommitsWithError++;
		break;

		default:
			errorString = "non-existent";
			break;
		}

	}

	public void printResults() {
		System.out.println(nbFileErrors + " files with errors (=not treated)");
		System.out.println(nbCommits + " commits");
		System.out.println(nbCommitsWithError + " commits with errors");
		System.out.println("_-_-_-_-_-_-_-_-_-_-_-");
		System.out.println(nbAssignment + " updates or insert of assignments");
		System.out
		.println(nbLocalVar + " updates or insert of local variables");
		System.out.println(nbReturn + " updates or insert of returns");
		System.out
		.println(nbFieldWrite + " updates or insert of field written");

	}

	public int getNbCommitsWithError() {
		return nbCommitsWithError;
	}

	public void setNbCommitsWithError(int nbCommitsWithError) {
		this.nbCommitsWithError = nbCommitsWithError;
	}

	public String getErrorString() {
		return errorString;
	}

	public void setError(String errorString) {
		this.errorString = errorString;
	}

	public int getNbFileErrors() {
		return nbFileErrors;
	}

	public void setNbFileErrors(int nbFileErrors) {
		this.nbFileErrors = nbFileErrors;
	}

	public int getNbCommits() {
		return nbCommits;
	}

	public void setNbCommits(int nbCommits) {
		this.nbCommits = nbCommits;
	}

	public int getNbAssignment() {
		return nbAssignment;
	}

	public void setNbAssignment(int nbAssignment) {
		this.nbAssignment = nbAssignment;
	}

	public int getNbLocalVar() {
		return nbLocalVar;
	}

	public void setNbLocalVar(int nbLocalVar) {
		this.nbLocalVar = nbLocalVar;
	}

	public int getNbReturn() {
		return nbReturn;
	}

	public void setNbReturn(int nbReturn) {
		this.nbReturn = nbReturn;
	}

	public int getNbFieldWrite() {
		return nbFieldWrite;
	}

	public void setNbFieldWrite(int nbFieldWrite) {
		this.nbFieldWrite = nbFieldWrite;
	}

	public int getCommitsWithError() {
		return nbCommitsWithError;
	}

	public void setCommitsWithError(int commitsWithError) {
		nbCommitsWithError = commitsWithError;
	}
}
