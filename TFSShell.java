import java.io.*;
import java.util.*;

public class TFSShell extends Thread
{

	//Creating and initializing TFSFileSystem object
	static TFSFileSystem  fs = new TFSFileSystem();

	public TFSShell()
	{
	}

	public void run()
	{
		readCmdLine();
	}

	/*
	 * User interface routine
	 */

	void readCmdLine()
	{
		String line, cmd, arg1, arg2, arg3, arg4;
		StringTokenizer stokenizer;
		Scanner scanner = new Scanner(System.in);

		System.out.println("Hal: Good morning, Dave!\n");

		while(true) {

			System.out.print("ush> ");

			line = scanner.nextLine();
			line = line.trim();
			stokenizer = new StringTokenizer(line);
			if (stokenizer.hasMoreTokens()) {
				cmd = stokenizer.nextToken();

				if (cmd.equals("mkfs"))
					mkfs();
				else if (cmd.equals("mount"))
					mount();
				else if (cmd.equals("unmount"))
					unmount();
				else if (cmd.equals("sync"))
					sync();
				else if (cmd.equals("prrfs"))
					prrfs();
				else if (cmd.equals("prmfs"))
					prmfs();

				else if (cmd.equals("mkdir")) {
					if (stokenizer.hasMoreTokens()) {
						arg1 = stokenizer.nextToken();
						mkdir(arg1);
					}
					else
						System.out.println("Usage: mkdir directory");
				}
				else if (cmd.equals("rmdir")) {
					if (stokenizer.hasMoreTokens()) {
						arg1 = stokenizer.nextToken();
						rmdir(arg1);
					}
					else
						System.out.println("Usage: rmdir directory");
				}
				else if (cmd.equals("ls")) {
					if (stokenizer.hasMoreTokens()) {
						arg1 = stokenizer.nextToken();
						ls(arg1);
					}
					else
						System.out.println("Usage: ls directory");
				}
				else if (cmd.equals("create")) {
					if (stokenizer.hasMoreTokens()) {
						arg1 = stokenizer.nextToken();
						create(arg1);
					}
					else
						System.out.println("Usage: create file");
				}
				else if (cmd.equals("rm")) {
					if (stokenizer.hasMoreTokens()) {
						arg1 = stokenizer.nextToken();
						rm(arg1);
					}
					else
						System.out.println("Usage: rm file");
				}
				else if (cmd.equals("print")) {
					if (stokenizer.hasMoreTokens())
						arg1 = stokenizer.nextToken();
					else {
						System.out.println("Usage: print file position number");
						continue;
					}
					if (stokenizer.hasMoreTokens())
						arg2 = stokenizer.nextToken();
					else {
						System.out.println("Usage: print file position number");
						continue;
					}
					if (stokenizer.hasMoreTokens())
						arg3 = stokenizer.nextToken();
					else {
						System.out.println("Usage: print file position number");
						continue;
					}
					try {
						print(arg1, Integer.parseInt(arg2), Integer.parseInt(arg3));
					} catch (NumberFormatException nfe) {
						System.out.println("Usage: print file position number");
					}
				}
				else if (cmd.equals("append")) {
					if (stokenizer.hasMoreTokens())
						arg1 = stokenizer.nextToken();
					else {
						System.out.println("Usage: append file number");
						continue;
					}
					if (stokenizer.hasMoreTokens())
						arg2 = stokenizer.nextToken();
					else {
						System.out.println("Usage: append file number");
						continue;
					}
					try {
						append(arg1, Integer.parseInt(arg2));
					} catch (NumberFormatException nfe) {
						System.out.println("Usage: append file number");
					}
				}
				else if (cmd.equals("cp")) {
					if (stokenizer.hasMoreTokens())
						arg1 = stokenizer.nextToken();
					else {
						System.out.println("Usage: cp file directory");
						continue;
					}
					if (stokenizer.hasMoreTokens())
						arg2 = stokenizer.nextToken();
					else {
						System.out.println("Usage: cp file directory");
						continue;
					}
					cp(arg1, arg2);
				}
				else if (cmd.equals("rename")) {
					if (stokenizer.hasMoreTokens())
						arg1 = stokenizer.nextToken();
					else {
						System.out.println("Usage: rename src_file dest_file");
						continue;
					}
					if (stokenizer.hasMoreTokens())
						arg2 = stokenizer.nextToken();
					else {
						System.out.println("Usage: rename src_file dest_file");
						continue;
					}
					rename(arg1, arg2);
				}

				else if (cmd.equals("exit")) {
					exit();
					System.out.println("\nHal: Good bye, Dave!\n");
					break;
				}

				else
					System.out.println("-ush: " + cmd + ": command not found");
			}
		}


	}


/*
 * You need to implement these commands
 */

 	//mkfs method:
 	// Creates file system.
	void mkfs()
	{
		fs.tfs_mkfs();
		return;
	}

	//mount method:
	//	Mounts file system
	void mount()
	{
		fs.tfs_mount();
		return;
	}

	//unmount method:
	//	Unmounts file system
	void unmount()
	{
		fs.tfs_umount();
		return;
	}

	//sync method:
	//	Syncs file system
	void sync()
	{
		fs.tfs_sync();
		return;
	}

	//prrfs method:
  // Prints out FAT and PCB in disk file.
	void prrfs()
	{
		System.out.println(fs.tfs_prrfs());
		return;
	}

	//prmfs method:
	//	Prints out FAT and PCB in memory
	void prmfs()
	{
		System.out.println(fs.tfs_prmfs());
		return;
	}

	//mkdir method:
	//	Make a directory if one does not exist
	void mkdir(String directory)
	{
		byte[] name = directory.getBytes();
		fs.tfs_create_dir(name, name.length);
		return;
	}

	//rmdir method:
	//	Remove a directory if it is empty
	void rmdir(String directory)
	{
		byte[] name = directory.getBytes();
		fs.tfs_delete_dir(name, name.length);
		return;
	}

	void ls(String directory)
	{
		return;
	}

	void create(String file)
	{
		return;
	}

	void rm(String file)
	{
		return;
	}

	void print(String file, int position, int number)
	{
		return;
	}

	void append(String file, int number)
	{
		return;
	}

	void cp(String file, String directory)
	{
		return;
	}

	void rename(String source_file, String destination_file)
	{
		return;
	}

	//exit method:
  // Closes the disk file and exits file system.
	void exit()
	{
		fs.tfs_exit();
		return;
	}
}


/*
 * main method
 */

class TFSMain
{
	public static void main(String argv[]) throws InterruptedException
	{
		TFSFileSystem tfs = new TFSFileSystem();
		TFSShell shell = new TFSShell();

		shell.start();
//		try {
			shell.join();
//		} catch (InterruptedException ie) {}
	}
}
