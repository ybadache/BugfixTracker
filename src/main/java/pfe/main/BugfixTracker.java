package pfe.main;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import com.github.gumtreediff.actions.model.Action;

import fr.inria.sacha.spoon.diffSpoon.CtDiff;
import fr.inria.sacha.spoon.diffSpoon.DiffSpoon;
import fr.inria.sacha.spoon.diffSpoon.DiffSpoonImpl;

/**
 * 
 * @author Yassine BADACHE
 *
 *         This class is used to track files in commit, and using the Gumtree
 *         AST Diff library given by Spoon to get the difference between the
 *         current and previous version of a commit, then extracting value from
 *         the numbers given.
 * 
 */
public class BugfixTracker {

	/** Various methods encapsulating methods to treats Git and commits datas */
	private BugfixTrackerUtils bugfixUtils;

	/** All the statistical datas (number of faulty commit, actions, etc) */
	private DataStatsHolder statsHolder;

	/** File managing object for tables */
	private DataResultsHolder resultsHolder;

	/** Name of the project */
	private String project;

	/** Owner of the project (necessary for Markdown parsing) */
	private String projectOwner;

	/** Path to the directory */
	private String directoryPath;

	/** Repository object, representing the directory */
	private Repository repository;

	/** Git entity to treat with the Repository datas */
	private Git git;

	/** Revision walker from JGit */
	private RevWalk rw;

	/** Classic constructor */
	public BugfixTracker(String[] args) throws Exception {
		projectOwner = args[0];
		project = args[1];

		// TODO: Make this user-friendly
		directoryPath = "../bugfixRepoSamples/" + project + "/.git";

		bugfixUtils = new BugfixTrackerUtils();
		statsHolder = new DataStatsHolder();
		repository = bugfixUtils.setRepository(directoryPath);
		git = new Git(repository);
		rw = new RevWalk(repository);
	}

	public void commitSampleTry(String ID) throws Exception {
		List<Action> totalactions = new ArrayList<Action>();
		List<Action> act = new ArrayList<Action>();
		List<String> debugging = new ArrayList<String>();
		String r = "";
		File debug = new File("debug-" + ID + ".txt");

		RevCommit commit = rw.parseCommit(repository.resolve(ID));

		if (commit.getParentCount() > 0) {
			RevCommit parent = rw.parseCommit(commit.getParent(0).getId());

			DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);

			df.setRepository(repository);
			df.setDiffComparator(RawTextComparator.DEFAULT);
			df.setDetectRenames(true);

			List<DiffEntry> diffs = df.scan(parent.getTree(), commit.getTree());

			for (DiffEntry diff : diffs) {
				String currentContent = bugfixUtils.getContent(repository, diff, commit)[0];
				String previousContent = bugfixUtils.getContent(repository, diff, commit)[1];

				if (diff.getNewPath().contains(".java")) {
					File f1 = bugfixUtils.writeContentInFile("c1.java", currentContent);
					File f2 = bugfixUtils.writeContentInFile("c2.java", previousContent);

					if (f1 != null && f2 != null) {
						try {
							DiffSpoon diffspoon = new DiffSpoonImpl();

							CtDiff result = diffspoon.compare(f1, f2);

							List<Action> actions = result.getRootActions();
							act = result.getRootActions();

							r = result.toString();

							for (Action a : actions) {
								totalactions.add(a);
							}

							f1.delete();
							f2.delete();
						}

						catch (Exception e) {
						}
					}
				}
				debugging.add("File being diffed : " + diff.getNewPath() + "\nActions : " + act.size() + "\n" + r.toString());
			}
			debugging.add("Total actions size : " + totalactions.size());
			FileUtils.writeStringToFile(debug, debugging.toString());
		}
	}

	/** Main method, probes all commits of a given repo and analyzes it */
	public void probeAllCommits() throws Exception {
		long startTime = System.nanoTime();
		resultsHolder = new DataResultsHolder(project, projectOwner, "all-commits");

		// List<Ref> branches = bugfixUtils.getAllBranches(git);

		Iterable<RevCommit> commits = bugfixUtils.getAllCommits(git);

		/** Goes through every commit of a given branch */
		for (RevCommit commit : commits) {
			boolean assigned = false;
			boolean returned = false;
			boolean fielded = false;
			boolean localed = false;
			String action = "";
			boolean faulty = false;
			List<Action> totalactions = new ArrayList<Action>();

			statsHolder.increment("commit");
			System.out.println("\n-------------------------------------");
			System.out.println("--- Files of commit n°" + statsHolder.getNbCommits() + " with ID : " + commit.getName());
			System.out.println("-------------------------------------");

			if (commit.getParentCount() > 0) {
				RevCommit targetCommit = rw.parseCommit(repository.resolve(commit.getName()));

				RevCommit targetParent = rw.parseCommit(commit.getParent(0).getId());

				DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);

				df.setRepository(repository);
				df.setDiffComparator(RawTextComparator.DEFAULT);
				df.setDetectRenames(true);

				List<DiffEntry> diffs = df.scan(targetParent.getTree(), targetCommit.getTree());

				for (DiffEntry diff : diffs) {
					String currentContent = bugfixUtils.getContent(repository, diff, commit)[0];
					String previousContent = bugfixUtils.getContent(repository, diff, commit)[1];

					if (diff.getNewPath().contains(".java")) {
						File f1 = bugfixUtils.writeContentInFile("c1.java", currentContent);
						File f2 = bugfixUtils.writeContentInFile("c2.java", previousContent);

						if (f1 != null && f2 != null) {
							try {
								DiffSpoon diffspoon = new DiffSpoonImpl();

								CtDiff result = diffspoon.compare(f1, f2);

								List<Action> actions = result.getRootActions();

								for (Action a : actions) {
									totalactions.add(a);
								}

								f1.delete();
								f2.delete();

								if (result.containsAction("Remove", "FieldWrite") || result.containsAction("Insert", "FieldWrite")
										|| result.containsAction("Update", "FieldWrite")) {
									if (!fielded) {
										resultsHolder.add("FieldWrite", commit);
										statsHolder.increment("FieldWrite");
									}
									fielded = true;

									action = "FieldWrite";
								}

								if (result.containsAction("Remove", "Assignment") || result.containsAction("Insert", "Assignment")
										|| result.containsAction("Update", "Assignment")) {
									if (!assigned) {
										resultsHolder.add("Assignment", commit);
										statsHolder.increment("Assignment");
									}
									assigned = true;

									action = "Assignment";
								}

								if (result.containsAction("Remove", "Return") || result.containsAction("Insert", "Return")
										|| result.containsAction("Update", "Return")) {
									if (!returned) {
										resultsHolder.add("Return", commit);
										statsHolder.increment("Return");
									}
									returned = true;

									action = "Return";
								}

								if (result.containsAction("Remove", "LocalVariable") || result.containsAction("Insert", "LocalVariable")
										|| result.containsAction("Update", "LocalVariable")) {
									if (!localed) {
										resultsHolder.add("LocalVariable", commit);
										statsHolder.increment("LocalVariable");
									}
									localed = true;

									action = "LocalVariable";
								}
							}

							catch (Exception e) {
								statsHolder.increment("file_error");
								faulty = true;
							}
						}
					}
				}
				if (faulty)
					statsHolder.increment("commit_error");

				if (totalactions.size() == 1) {
					resultsHolder.addOneOnly(action, commit);
					statsHolder.incrementOnlyOne(action);

					System.out.println("Hello !");
					action = "";
				}
			}

			if (statsHolder.getNbCommits() % 20 == 0) {
				System.out.println("Save !");
				statsHolder.saveResults(project, "all-commits");
				resultsHolder.saveResults();
			}
		}

		statsHolder.printResults();
		resultsHolder.saveResults();

		statsHolder.reset();
		long endTime = System.nanoTime();

		long duration = (endTime - startTime) / 1000000;
		System.out.println("Execution time : " + duration + "ms (" + duration / 1000 + "s)");
	}

	public void probeOddCodeCommit(String filepath) throws Exception {
		long startTime = System.nanoTime();
		resultsHolder = new DataResultsHolder(project, projectOwner, "odd-code");

		for (String line : Files.readAllLines(Paths.get("../bugfixRepoSamples/" + project + "/" + filepath))) {
			boolean assigned = false;
			boolean returned = false;
			boolean fielded = false;
			boolean localed = false;
			String action = "";
			boolean faulty = false;

			List<Action> totalactions = new ArrayList<Action>();
			String[] parts = line.split(",");

			System.out.println("\n-------------------------------------");
			System.out.println("--- Files of commit " + parts[0]);
			System.out.println("-------------------------------------");

			RevCommit bf_sha = rw.parseCommit(repository.resolve(parts[0]));
			RevCommit bi_sha = rw.parseCommit(repository.resolve(parts[1]));

			DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);

			df.setRepository(repository);
			df.setDiffComparator(RawTextComparator.DEFAULT);
			df.setDetectRenames(true);

			List<DiffEntry> diffs = df.scan(bf_sha.getTree(), bi_sha.getTree());

			for (DiffEntry diff : diffs) {
				String currentContent = bugfixUtils.getContent(repository, diff, bf_sha)[0];
				String previousContent = bugfixUtils.getContent(repository, diff, bi_sha)[0];

				if (diff.getNewPath().contains(".java")) {
					File f1 = bugfixUtils.writeContentInFile("c1.java", currentContent);
					File f2 = bugfixUtils.writeContentInFile("c2.java", previousContent);

					if (f1 != null && f2 != null) {
						try {
							DiffSpoon diffspoon = new DiffSpoonImpl();

							CtDiff result = diffspoon.compare(f1, f2);

							List<Action> actions = result.getRootActions();

							for (Action a : actions) {
								totalactions.add(a);
							}

							f1.delete();
							f2.delete();

							/*
							 * First checking : if it contains an indicated
							 * action
							 */
							if (result.containsAction("Remove", "LocalVariable") || result.containsAction("Insert", "FieldWrite")
									|| result.containsAction("Update", "FieldWrite")) {
								if (!fielded) {
									resultsHolder.add("FieldWrite", bf_sha);
									statsHolder.increment("FieldWrite");
								}
								fielded = true;

								action = "FieldWrite";
							}

							if (result.containsAction("Remove", "LocalVariable") || result.containsAction("Insert", "Assignment")
									|| result.containsAction("Update", "Assignment")) {
								if (!assigned) {
									resultsHolder.add("Assignment", bf_sha);
									statsHolder.increment("Assignment");
								}
								assigned = true;

								action = "Assignment";
							}

							if (result.containsAction("Remove", "LocalVariable") || result.containsAction("Insert", "Return")
									|| result.containsAction("Update", "Return")) {
								if (!returned) {
									resultsHolder.add("Return", bf_sha);
									statsHolder.increment("Return");
								}
								returned = true;

								action = "Return";
							}

							if (result.containsAction("Remove", "LocalVariable") || result.containsAction("Insert", "LocalVariable")
									|| result.containsAction("Update", "LocalVariable")) {
								if (!localed) {
									resultsHolder.add("LocalVariable", bf_sha);
									statsHolder.increment("LocalVariable");
								}
								localed = true;

								action = "LocalVariable";
							}
						}

						catch (Exception e) {
							statsHolder.increment("file_error");
							faulty = true;
						}
					}
				}
			}
			if (faulty)
				statsHolder.increment("commit_error");

			if (totalactions.size() == 1) {
				resultsHolder.addOneOnly(action, bf_sha);
				statsHolder.incrementOnlyOne(action);

				action = "";
			}

			if (statsHolder.getNbCommits() % 20 == 0) {
				System.out.println("Save !");
				statsHolder.saveResults(project, "odd-code");
				resultsHolder.saveResults();
			}
		}

		statsHolder.printResults();
		resultsHolder.saveResults();

		statsHolder.reset();
		long endTime = System.nanoTime();

		long duration = (endTime - startTime) / 1000000;
		System.out.println("Execution time : " + duration + "ms (" + duration / 1000 + "s)");
	}

	public void probeFileCommit(String filepath) throws Exception {
		resultsHolder = new DataResultsHolder(project, projectOwner, "xmls-commits");

		for (String line : Files.readAllLines(Paths.get("../bugfixRepoSamples/" + project + "/" + filepath))) {
			boolean assigned = false;
			boolean returned = false;
			boolean fielded = false;
			boolean localed = false;
			String action = "";
			boolean faulty = false;
			List<Action> totalactions = new ArrayList<Action>();
			// System.out.println("\n-------------------------------------");
			// System.out.println("--- Files of commit " + line);
			// System.out.println("-------------------------------------");

			RevCommit commit = rw.parseCommit(repository.resolve(line));

			if (commit.getParentCount() > 0) {
				RevCommit parent = rw.parseCommit(commit.getParent(0).getId());

				DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);

				df.setRepository(repository);
				df.setDiffComparator(RawTextComparator.DEFAULT);
				df.setDetectRenames(true);

				List<DiffEntry> diffs = df.scan(commit.getTree(), parent.getTree());

				for (DiffEntry diff : diffs) {
					String currentContent = bugfixUtils.getContent(repository, diff, commit)[0];
					String previousContent = bugfixUtils.getContent(repository, diff, commit)[1];

					if (diff.getNewPath().contains(".java")) {
						File f1 = bugfixUtils.writeContentInFile("c1.java", currentContent);
						File f2 = bugfixUtils.writeContentInFile("c2.java", previousContent);

						if (f1 != null && f2 != null) {
							try {
								DiffSpoon diffspoon = new DiffSpoonImpl();

								CtDiff result = diffspoon.compare(f1, f2);

								List<Action> actions = result.getRootActions();

								for (Action a : actions) {
									totalactions.add(a);
								}

								f1.delete();
								f2.delete();

								/*
								 * First checking : if it contains an indicated
								 * action
								 */
								if (result.containsAction("Remove", "LocalVariable") || result.containsAction("Insert", "FieldWrite")
										|| result.containsAction("Update", "FieldWrite")) {
									if (!fielded) {
										resultsHolder.add("FieldWrite", commit);
										statsHolder.increment("FieldWrite");
									}
									fielded = true;

									action = "FieldWrite";
									// System.out.println("Changer value: " +
									// nbchanges + "(" + action + ")");
								}

								if (result.containsAction("Remove", "LocalVariable") || result.containsAction("Insert", "Assignment")
										|| result.containsAction("Update", "Assignment")) {
									if (!assigned) {
										resultsHolder.add("Assignment", commit);
										statsHolder.increment("Assignment");
									}
									assigned = true;

									action = "Assignment";
								}

								if (result.containsAction("Remove", "LocalVariable") || result.containsAction("Insert", "Return")
										|| result.containsAction("Update", "Return")) {
									if (!returned) {
										resultsHolder.add("Return", commit);
										statsHolder.increment("Return");
									}
									returned = true;

									action = "Return";
								}

								if (result.containsAction("Remove", "LocalVariable") || result.containsAction("Insert", "LocalVariable")
										|| result.containsAction("Update", "LocalVariable")) {
									if (!localed) {
										resultsHolder.add("LocalVariable", commit);
										statsHolder.increment("LocalVariable");
									}
									localed = true;

									action = "LocalVariable";
								}
							}

							catch (Exception e) {
								statsHolder.increment("file_error");
								faulty = true;
							}
						}
					}
				}
				if (faulty)
					statsHolder.increment("commit_error");

				if (totalactions.size() == 1) {
					System.out.println("Hello !");
					resultsHolder.addOneOnly(action, commit);
					statsHolder.incrementOnlyOne(action);

					action = "";
				}

				if (statsHolder.getNbCommits() % 20 == 0) {
					System.out.println("Save !");
					statsHolder.saveResults(project, "xmls-commits");
					resultsHolder.saveResults();
				}
			}
			statsHolder.saveResults(project, "xmls-commits");
			resultsHolder.saveResults();

			statsHolder.reset();
		}
	}

	/*
	 * Getters and setters Below this point
	 */
	public BugfixTrackerUtils getBugfixUtils() {
		return bugfixUtils;
	}

	public void setBugfixUtils(BugfixTrackerUtils bugfixUtils) {
		this.bugfixUtils = bugfixUtils;
	}

	public DataStatsHolder getStatsHolder() {
		return statsHolder;
	}

	public void setStatsHolder(DataStatsHolder statsHolder) {
		this.statsHolder = statsHolder;
	}

	public DataResultsHolder getResultsHolder() {
		return resultsHolder;
	}

	public void setResultsHolder(DataResultsHolder resultsHolder) {
		this.resultsHolder = resultsHolder;
	}

	public String getProject() {
		return project;
	}

	public void setProject(String project) {
		this.project = project;
	}

	public String getProjectOwner() {
		return projectOwner;
	}

	public void setProjectOwner(String projectOwner) {
		this.projectOwner = projectOwner;
	}

	public String getDirectoryPath() {
		return directoryPath;
	}

	public void setDirectoryPath(String directoryPath) {
		this.directoryPath = directoryPath;
	}

	public Repository getRepository() {
		return repository;
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	public Git getGit() {
		return git;
	}

	public void setGit(Git git) {
		this.git = git;
	}

	public RevWalk getRw() {
		return rw;
	}

	public void setRw(RevWalk rw) {
		this.rw = rw;
	}
}
