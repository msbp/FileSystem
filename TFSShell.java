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
		byte[] name = directory.getBytes();
		int fd = fs.tfs_open(name, name.length); //Opening FD entry
		byte[] is_directory = new byte[10];
		byte[] nlength = new byte[10]; //Won't have more than 10 entries - Assumption: this could be changed
		byte[][] name_arr = new byte[10][16];
		int[] first_block_no = new int[10];
		int[] file_size = new int[10];

		//Number of entries
		int numEntries = fs.tfs_read_dir(fd, is_directory, nlength, name_arr, first_block_no, file_size);

		System.out.println("\nEntries in directory:");
		//Iterate and print out entries
		for (int i = 0; i < numEntries; i++){
			System.out.println("\t"+ new String(name_arr[i]) + "\tis_directory: "+ is_directory[i] + "\tFirstBlockNo: " + first_block_no[i] + "\tSize: " + file_size[i]+"bytes");
		}
		System.out.println("\n");

		return;
	}

	//create method:
	//	Create an empty file if it does not exits
	void create(String file)
	{
		byte[] name = file.getBytes();
		fs.tfs_create(name, name.length);
		return;
	}

	//rm method:
	//	Removes a file
	void rm(String file)
	{
		byte[] name = file.getBytes();
		fs.tfs_delete(name, name.length);
		return;
	}

	void print(String file, int position, int number)
	{
		byte[] name = file.getBytes();
		int fd = fs.tfs_open(name, name.length); //Opening fd entry
		byte[] buffer = new byte[number*2]; //Each char is 2 bytes

		System.out.println("\nRead from file:");

		int bytesRead = fs.tfs_read(fd, buffer, buffer.length);
		byte[] tmp = new byte[2];
		for (int i = 0; i < bytesRead; i = i+2){
			tmp[0] = buffer[i];
			tmp[1] = buffer[i+1];
			System.out.print(new String(tmp));
		}
		System.out.println("\n");
		return;
	}

	void append(String file, int number)
	{
		byte[] name = file.getBytes();
		int fd = fs.tfs_open(name, name.length); //Opening fd entry

		if (fd == -1){
			System.out.println("File does not exist.");
			return;
		}

		String data = "";
		for (int i = 0; i < number; i++){
			data += "D"; //Adding random character Number times
		}
		byte[] buf = data.getBytes();

		fs.tfs_write(fd, buf, buf.length);
		return;
	}

	void cp(String file, String directory)
	{
		byte[] sourceName = file.getBytes();
		byte[] destinationName = directory.getBytes();
		int fd1 = fs.tfs_open(sourceName, sourceName.length); //Opening fd entry

		if (fd1 == -1){
			System.out.println("Source file does not exist");
			return;
		}

		fs.tfs_create(destinationName, destinationName.length); //Creating file entry
		int fd2 = fs.tfs_open(destinationName, destinationName.length); //Opening FD
		byte[] buf = new byte[128];
		//Copying data
		fs.tfs_read(fd1, buf, buf.length);
		fs.tfs_write(fd2, buf, buf.length);

		return;
	}

	void rename(String source_file, String destination_file)
	{
		byte[] sourceName = source_file.getBytes();

		int entryNo = fs._tfs_search_dir(sourceName, sourceName.length);

		if (entryNo == -1){
			System.out.println("There was an error finding the source file.");
			return;
		}

		String[] path = source_file.split("/"); //Creating a string array with the path
		byte[] n = path[path.length-1].getBytes();

		byte[] is_directory = new byte[1];
		int[] fbn = new int[1];
		int[] size = new int[1];
		if (fs._tfs_get_entry_dir(entryNo, n, (byte)n.length, is_directory, fbn, size) == -1){
			System.out.println("There was an error.");
			return;
		}

		if (fs._tfs_update_entry_dir(entryNo, n, (byte)n.length, is_directory[0], fbn[0], size[0]) == -1){
			System.out.println("There was an error.");
			return;
		}

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
