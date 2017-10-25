/*
	Data Structures used:
		PCB: The Partition Control Block, or SuperBlock was implemented as
				 an array of bytes within a PCB class. When a PCB object is
				 created, the class constructor initializes the byte array and
				 populates it with the right data. Using a class for this allows
				 for another constructor to be created that takes as a parameter
				 the PCB block from the disk and creates an in-memory object of it
				 for quicker access. Each 4 bytes of the array translates to an
				 integer. index 0 to 3 holds the FAT size, index 4 to 7 holds
				 pointer to root directory and index 8 to 11 holds a pointer to
				 the first free block.
		FAT: The File Allocation Table was implemented as an array of bytes within
				 a FAT class. When a FAT object is created, the class constructor
				 initializes an int array and each index in the array corresponds to a
				 block in memory where a file is located. Each index holds a pointer to
				 the index of where the rest of the file is located and so on. This int
				 array is turned into a byte array, every integer is represents by 4
				 bytes. This byte array is used when the FAT is written to memory. Just
				 like the PCB, having an exclusive FAT class allows for another
				 constructor to be created later that takes the FAT block from disk as
				 a parameter and creates an in-memory object for quicker access.
		Directory: The directory was implemented using a linked list and uses a
				 class of its own. Every directory is created as an object (list) of its
				 own and then appended to its parent directory using an in-class method.
				 The root directory initialized in-memory in tfs_mkfs will be written to
				 to the disk as a byte array.
				 ***
				 ***
				 ***
				 The directory structure was updated to be written to disk. When a
				 directory is created, the entry is written to the disk. Directory is not
				 kept in memory in a linked list anymore, as we have a small file System
				 accessing the in disk directory is done pretty quickly. Each directory
				 has a starting block that holds more directory entries or file entries.
				 These directory entries point to their own blocks, holding more directories
				 or file entries. The file entries point to their starting block, where
				 their content can be read from.
				 ***
				 ***
				 ***
		FDT: The File Descriptor Table was implemented as a linked list of
				 FileDescriptor objects. As these objects are created, they are appended
				 to the linked list and given an index on the list. This index is the
				 reference fd number that the methods use to reference a file
				 descriptor. The FileDescriptor object contains many attributes about
				 an opened file in the system.
*/


import java.io.*;
import java.util.*;

public class TFSFileSystem
{
	static final String DISK_FILE = "TFSDiskFile"; //Name of Disk file to be created
	static final int DISK_FILE_SIZE = 2048; //Number of blocks
	static final int BLOCK_SIZE = 128; //Number of byes per block

	static TFSDiskInputOutput disk = new TFSDiskInputOutput();
	static PCB pcb; //Creating Partition Control Block
	static FAT fat; //Creating File Allocation Table
	static Directory root; //Creating Root directory

	static List<FileDescriptor> fdt = new LinkedList<FileDescriptor>(); //Declaring File Descriptor table (implemented as a list)

	 //Main method:
	 // Used for testing purposes. Some commented out code to keep things
	 // clear.
	 public static void main(String args[]){

		 //Initializing objects - for testing purposes
		 //byte[] name = DISK_FILE.getBytes();
		 //disk.tfs_dio_create(name, name.length, DISK_FILE_SIZE);
		 //disk.tfs_dio_open(test, test.length);
		TFSFileSystem tru = new TFSFileSystem();

		 //Initializing file system
		 tru.tfs_mkfs();

		 System.out.println(tru.tfs_prmfs());

	 }

	 /*
 	 * TFS Constructor
 	 */
	public void TFSFileSystem()
	{

	}


	/*
	 * TFS API
	 */
	//tfs_mkfs method:
	// Opens the disk file, creates and initializes PCB, FAT and
	// Directory objects in memory. It also writes PCB and FAT to
	// disk.
	public static int tfs_mkfs()
	{
		//Try to open disk open if there is an error, return -1
		byte[] name = DISK_FILE.getBytes();
		int response = disk.tfs_dio_open(name, name.length);
		if (response == -1){
			return response; //Return error if cannot open
		}

		//initialize PCB object in memory
		pcb = new PCB(DISK_FILE_SIZE, BLOCK_SIZE);
		//initialize FAT object in memory
		fat = new FAT(pcb.fatSize, BLOCK_SIZE);
		//initialize root Directory object in memory
		String str = "/";
		byte[] b_str = str.getBytes();
		byte[] n = new byte[16];
		for (int i = 0; i < b_str.length; i++){
			n[i] = b_str[i];
		}
		//Root initialized at block 67
		root = new Directory(n, (byte)n.length, (byte)0, 67, 0); //Starting at block 68 (root) Size = 0 because it is empty at beginning
		//Write the directory to disk
		byte[] tmp = new byte[BLOCK_SIZE];
		_tfs_read_block(67, tmp);
		_tfs_put_bytes_block(tmp, 0, root.dirBlock, root.dirBlock.length);


		//Update FAT since root is on block 67 now
		fat.fatTable[67] = -1;//Points to -1 since it is also the end of the file
		//fat.updateFATBlocks();
		fat.updateBlocksFromTable(67, -1);

		int freeBlock = fat.findFreeBlock(); //Find new free block
		//If free block returned -1 then there are not blocks available
		if (freeBlock == -1){
			System.out.println("There are no blocks available in FAT.");
			return -1;
		}
		pcb.updateFreeBlockPointer(freeBlock);


		//Writing PCB and FAT from memory to disk
		tfs_sync();


		//Writing root directory to disk at block 67
		_tfs_create_entry_dir(68, root.name, root.nLength, root.isDirectory, root.firstBlockNo+1, root.size);

		return 0;
	}

	//tfs_exit method:
	// Calls tfs_unmount and closes the disk file.
	// Returns a string.
	public static String tfs_exit(){
		tfs_umount();
		disk.tfs_dio_close();
		return "tfs_exit from TFSFileSystem.java called.";
	}

	//tfs_mount method:
	//	Writes PCB and FAT to disk
	public static int tfs_mount()
	{
		//Write PCB from memory to disk
		_tfs_write_pcb();
		//Write FAT from memory to disk
		_tfs_write_fat();
		return 0;
	}

	//tfs_umount method:
	//	Reads PCB and FAT from disk to memory
	public static int tfs_umount()
	{
		//Read PCB
		_tfs_read_pcb();
		//Read FAT
		_tfs_read_fat();
		return 0;
	}

	//tfs_sync method:
	//	Synchronizes the file system (memory with disk)
	public static int tfs_sync()
	{
		_tfs_write_pcb();
		_tfs_write_fat();
		return 0;
	}

	//tfs_prrfs method:
	// Loads PCB and FAT from disk into a buffer.
	// Returns both in a string.
	public static String tfs_prrfs()
	{
		String blocksInDisk = null;

		byte[] pcbBuffer = new byte[BLOCK_SIZE]; //Buffer for pcb block - PCB is one block in length
		byte[] fatBuffer = new byte[BLOCK_SIZE]; //Buffer for fat of length of fat

		_tfs_read_block(1, pcbBuffer);

		//Saving root pointer from memory retrieved pcb buffer into variable
		int fatSize = (((pcbBuffer[0] & 0xFF) << 24)|((pcbBuffer[1] & 0xFF) << 16)|((pcbBuffer[2] & 0xFF) << 8)|(pcbBuffer[3] & 0xFF))*4/128;
		//Saving free block pointer from memory retrieved pcb buffer into variable
		int rootPointer = (((pcbBuffer[4] & 0xFF) << 24)|((pcbBuffer[5] & 0xFF) << 16)|((pcbBuffer[6] & 0xFF) << 8)|(pcbBuffer[7] & 0xFF));
		//Saving fat size from pointer from memory retrieved pcb buffer into variable
		int freeBlockPointer = (((pcbBuffer[8] & 0xFF) << 24)|((pcbBuffer[9] & 0xFF) << 16)|((pcbBuffer[10] & 0xFF) << 8)|(pcbBuffer[11] & 0xFF));

		blocksInDisk = "\nIn File System:\n";
		blocksInDisk += "PCB:\nRoot Pointer (block #): " + rootPointer +  "\tFirst Free Block: " + freeBlockPointer +  "\tSize of FAT (blocks): " + fatSize + "\n";
		blocksInDisk += "FAT:\n";

		//Iterate through fatBlocks in disk file and append it to string
		byte[] tmp = new byte[4];
		int num;
		//Iterating to read each block of fat starting at block 2
		for (int i = 2; i < fat.numBlocks+2; i++){
			_tfs_read_block(i, fatBuffer); //Reading FAT block into buffer
			//Iterating through fatBuffer and translating bytes to ints into returning string
			for (int j = 0; j < fatBuffer.length/4; j++) { //Divided by 4 because each int is represented by 4 bytes
				//Storing four bytes that form an int into tmp
				tmp[0] = fatBuffer[j*4]; tmp[1] = fatBuffer[(j*4)+1]; tmp[2] = fatBuffer[(j*4)+2]; tmp[3] = fatBuffer[(j*4)+3];
				//Converting 4 bytes into int
				num = (((tmp[0] & 0xFF) << 24)|((tmp[1] & 0xFF) << 16)|((tmp[2] & 0xFF) << 8)|(tmp[3] & 0xFF));
				//Appending it to returning string
				blocksInDisk += (j + ((i-2)*BLOCK_SIZE/4) + ": " + num + "\t");
			}
		}
		return blocksInDisk;
	}

	//tfs_prmfs method:
	//	Writes PCB and FAT from disk to a string that is returned
	public static String tfs_prmfs()
	{
		//Building string to be returned
		String inMemory = "\nIn Memory:\n";
		inMemory += "PCB:\nRoot Pointer (block #): " + pcb.rootPointer + "\tFirst Free Block: " + pcb.freeBlockPointer + "\tSize of FAT (blocks): " + pcb.numFatBlocks + "\n";
		inMemory += "FAT:\n";
		//Itereating through fat and appending each value
		for (int i = 0; i < fat.fatTable.length; i++){
			inMemory += i + ": " + fat.fatTable[i] + "\t";
		}
		return inMemory;
	}

	//tfs_open method:
	//	Opens a file descriptor entry into fdt
	public static int tfs_open(byte[] name, int nlength)
	{
		return _tfs_open_fd(name, nlength );
	}

	//tfs_read_dir() method:
	//	Reads directory entries into arrays
	public static int tfs_read_dir(int fd, byte[] is_directory, byte[] nlength, byte[][] name, int[] first_block_no, int[] file_size){
		//Create FileDescriptor object from fd number given
		FileDescriptor f = fdt.get(fd); //Creating a reference to it
		byte[] tmp = new byte[BLOCK_SIZE]; //This is where bytes will be temporarily stored
		byte[] bDir = new byte[32];
		int entry = f.startingBlock; //This is where the entries are held
		boolean empty = true;
		int count = 0;

		while (true){
			_tfs_read_block(entry, tmp);
			//Iterate through entries and saving them to array
			for (int i = 0; i < 4; i++){
				empty = true; //Reset variable
				//Get directory entry
				bDir = _tfs_get_bytes_block(tmp, (i*32), 32);

				//If there is an entry then save it
				//If it is empty then move to next entry
				for (int j = 0; j < 32; j++){
					if (bDir[j] != 0){
						empty = false; //If we get here, that means the entry is not empty
					}
				}
				if (empty == true){
					continue; //If entry is empty, go to next entry
				}
				//If we get to this part of the code, it means the entry is not empty
				//is_directory, nlength, name[][], first_block_no[] int, file_size[] int
				is_directory[count] = bDir[4];
				nlength[count] = bDir[5];
				for (int k = 8; k < 24; k++){
					name[count][k-8] = bDir[k];
				}
				first_block_no[count] = (((bDir[24] & 0xFF) << 24)|((bDir[25] & 0xFF) << 16)|((bDir[26] & 0xFF) << 8)|(bDir[27] & 0xFF)); //Retrieve first block number of directory entries
				file_size[count] = (((bDir[28] & 0xFF) << 24)|((bDir[29] & 0xFF) << 16)|((bDir[30] & 0xFF) << 8)|(bDir[31] & 0xFF)); //Retrieve filesize of directory entries

				count++; //Increment count variable
			}
			int nextBlock = fat.fatTable[entry];
			if (nextBlock == -1){
				return count;
			}
			entry = nextBlock;
		}

	}


	//tfs_read method:
	//	Read blength bytes in buf from file_id
	//	Returns the number of bytes read
	public static int tfs_read(int file_id, byte[] buf, int blength)
	{
		return _tfs_read_bytes_fd(file_id, buf, blength);
	}

	//tfs_write method:
	//	Writes blength bytes of buf in memory
	//	Returns the number of bytes written
	public static int tfs_write(int file_id, byte[] buf, int blength)
	{
		return _tfs_write_bytes_fd(file_id, buf, blength);
	}

	//tfs_seek method:
	//	returns new file pointer
	public static int tfs_seek(int file_id, int position)
	{
		return _tfs_seek_fd(file_id, position);
	}

	//tfs_close method:
	//	Removes the file descriptor from File Descriptor Table (FDT)
	public static void tfs_close(int file_id)
	{
		_tfs_close_fd(file_id);
		return;
	}

	//Helper method for tfs_create()
	public static int helper_tfs_create(byte[] name, int nlength){
		String str = new String(name); //Creating a string from name
		String[] path = str.split("/"); //Creating a string array with the path
		//If first character is not root then we don't have full path
    if (str.charAt(0) != '/'){
      return -1;
    }

		//Find block that root belongs to => Root is located on block 67
		//Retriever block 67
		byte[] bDir = new byte[32]; //directory entry holder
		byte[] tmp = new byte[BLOCK_SIZE]; //tmp block holder
		byte[] n = new byte[16]; //name holder

		int currName = 1;
		boolean found = false;

		//Reads root, as we are always starting at root
		int entry = 67;
		_tfs_read_block(entry, tmp);
		bDir = _tfs_get_bytes_block(tmp, 0, 32);
		//Retrieving size of directory (size of root right now)
		int sizeOfDir = (((bDir[28] & 0xFF) << 24)|((bDir[29] & 0xFF) << 16)|((bDir[30] & 0xFF) << 8)|(bDir[31] & 0xFF));
		sizeOfDir = sizeOfDir/BLOCK_SIZE; //Amount of entries in each directory
		//Retrieving first block number of root directory
		int firstBlockNo = (((bDir[24] & 0xFF) << 24)|((bDir[25] & 0xFF) << 16)|((bDir[26] & 0xFF) << 8)|(bDir[27] & 0xFF)); //Retrieve first block number of directory entries

		//Reading root directory items
		_tfs_read_block(firstBlockNo, tmp);

		while (true) {
			found = false; //Setting false variable to false
			//Iterate through
			for (int i = 0, secondI = 0; i < sizeOfDir; i++, secondI++){
				//If we read 4 entries already, we need to look at the following block that contains the rest of directory entries
				if (i%4 == 0){
					int nextBlock = fat.fatTable[entry];
					if (nextBlock == -1){
						return -1;
					}
					entry = nextBlock;
					_tfs_read_block(nextBlock, tmp);
					secondI = 0;
				}
				//Get the entry
				bDir = _tfs_get_bytes_block(tmp, (secondI*32), 32);

				//Compare the names
				for (int j = 8; j < 24; j++){
					n[j-8] = bDir[j];
				}
				String strName = new String(n);
				if (strName.equals(path[currName])){
					//If we are at the end of the path and we are at the last entry then return the parent block
					if (path.length-2 == currName){
						found = true;
						return (((bDir[24] & 0xFF) << 24)|((bDir[25] & 0xFF) << 16)|((bDir[26] & 0xFF) << 8)|(bDir[27] & 0xFF)); //Retrieve first block number of directory entry
					}
					found = true;
					currName++;
					sizeOfDir = (((bDir[28] & 0xFF) << 24)|((bDir[29] & 0xFF) << 16)|((bDir[30] & 0xFF) << 8)|(bDir[31] & 0xFF));
					sizeOfDir = sizeOfDir/BLOCK_SIZE;
					firstBlockNo = (((bDir[24] & 0xFF) << 24)|((bDir[25] & 0xFF) << 16)|((bDir[26] & 0xFF) << 8)|(bDir[27] & 0xFF)); //Retrieve first block number of directory entries
					_tfs_read_block(firstBlockNo, tmp); //Reading new block
					break; //Break out of first loop and look into next directory
				}
			}
			//If we arrive at the end of the loop, we hav
			if (found == false){
				return -1;
			}

		}



	}

	//tfs_create method:
	//	Create a file, name contains full path
	//	Returns file descriptor entry
	public static int tfs_create(byte[] name, int nlength)
	{
		//Find the block number of parent directory
		int block_no = helper_tfs_create(name, nlength); //Gets firstBlockNo of parent directory of name

		String newName = new String(name); //Creating a string from name
		String[] path = newName.split("/"); //Creating a string array with the path
		byte[] n = path[path.length-1].getBytes();

		//The method will allocate the entry on table for it
		_tfs_create_entry_dir(block_no, n, (byte)n.length, (byte)1, pcb.freeBlockPointer, 0);
		return tfs_open(name, nlength); //Creates a FileDescriptor in FDT for the new file
	}

	//tfs_delete method:
	//	Delete a file, name contains full path
	public static int tfs_delete(byte[] name, int nlength)
	{
		int parent_blockNo = _tfs_search_dir(name, nlength); //Getting parent block number
		//Get name of the file only, not entire path
		String newName = new String(name);
		String path[] = newName.split("/");
		byte[] n = path[path.length-1].getBytes();

		return _tfs_delete_entry(parent_blockNo, n, (byte)n.length);
	}

	//tfs_create_dir method:
	// Create a directory, name contains full path
	public static int tfs_create_dir(byte[] name, int nlength)
	{
		//Find the block number of parent directory
		int block_no = helper_tfs_create(name, nlength); //Gets firstBlockNo of parent directory of name

		String newName = new String(name); //Creating a string from name
		String[] path = newName.split("/"); //Creating a string array with the path
		byte[] n = path[path.length-1].getBytes();

		//The method will allocate the entry on table for it
		_tfs_create_entry_dir(block_no, n, (byte)n.length, (byte)0, pcb.freeBlockPointer, 0);
		return tfs_open(name, nlength); //Creates a FileDescriptor in FDT for the new file
	}

	//tfs_delete_dir method:
	//	Deletes a directory, name contains full path
	public static int tfs_delete_dir(byte[] name, int nlength)
	{
		int parent_blockNo = _tfs_search_dir(name, nlength); //Getting parent block number
		//Get name of the file only, not entire path
		String newName = new String(name);
		String path[] = newName.split("/");
		byte[] n = path[path.length-1].getBytes();

		return _tfs_delete_entry(parent_blockNo, n, (byte)n.length);
	}


	/*
	 * TFS private methods to handle in-memory structures
	 */

 	private static int _tfs_read_block(int block_no, byte buf[])
 	{
		//Calling TFSDiskInputOuput read method
		int response = disk.tfs_dio_read_block(block_no, buf);
 		return response; //Returning response from method
 	}

 	private static int _tfs_write_block(int block_no, byte buf[])
 	{
		//Calling TFSDiskInputOutput write method
		int response = disk.tfs_dio_write_block(block_no, buf);
 		return response; //Returning response from method
 	}

	//_tfs_open_fd method:
	//	Create a new entry in File Descriptor Table for a file or directory
	//	Returns file descriptor
 	private static int _tfs_open_fd(byte name[], int nlength)
 	{
		int blockNumber = _tfs_search_dir(name, nlength);
		byte[] is_directory = new byte[1];
		int[] fbn = new int[1];
		int[] size = new int[1];
		_tfs_get_entry_dir(blockNumber, name, (byte)nlength, is_directory, fbn, size);
		//Creating File Descriptor object
		FileDescriptor fd = new FileDescriptor(name, nlength, is_directory[0], fbn[0], size[0]);
		fdt.add(fd); //Adding it to the File Descriptor Table (Implemented as a linked list)
 		return (fdt.size() - 1); //Returning index of file descriptor
 	}

	//_tfs_seek_fd method:
	//	Change the file pointer to offset
	//	Returns the file pointer
	//	NOTE: offset variable is based on indexes. These indexes go from 0 to n-1.
 	private static int _tfs_seek_fd(int fd, int offset)
 	{
		//If offset is not valid, return error
		if (offset < 0){
			return -1; //-1 is error
		}
		fdt.get(fd).filePointer = offset;
 		return fdt.get(fd).filePointer;
 	}

	//_tfs_close_fd method:
	//	Remove FileDescriptor of index fd from File Descriptor Table.
	//	Updates the entry for the file/directory in the parent directory
 	private static void _tfs_close_fd(int fd)
 	{
		FileDescriptor f = fdt.get(fd);
		//Updates dir entry
		_tfs_update_entry_dir(f.startingBlock, f.name, (byte)f.name.length, f.isDirectory, f.startingBlock, f.fileSize);

		fdt.remove(fd); //Removes FileDescriptor object from FDT
 		return;
 	}

	//_tfs_search_dir method:
	//	Returns the first block number of the parent directory in which name exists
	public static int _tfs_search_dir(byte[] name, int nlength){
		int parentBlockNo = 67; //This will be returned - It is the first block of the parent -- Initialize it to point to root block
		String str = new String(name); //Creating a string from name
		String[] path = str.split("/"); //Creating a string array with the path
		//If first character is not root then we don't have full path
    if (str.charAt(0) != '/'){
      return -1;
    }

		//Find block that root belongs to => Root is located on block 67
		//Retriever block 67
		byte[] bDir = new byte[32]; //directory entry holder
		byte[] tmp = new byte[BLOCK_SIZE]; //tmp block holder
		byte[] n = new byte[16]; //name holder

		int currName = 1;
		boolean found = false;

		//Reads root, as we are always starting at root
		int entry = 67;
		_tfs_read_block(entry, tmp);
		bDir = _tfs_get_bytes_block(tmp, 0, 32);
		//Retrieving size of directory (size of root right now)
		int sizeOfDir = (((bDir[28] & 0xFF) << 24)|((bDir[29] & 0xFF) << 16)|((bDir[30] & 0xFF) << 8)|(bDir[31] & 0xFF));
		sizeOfDir = sizeOfDir/BLOCK_SIZE; //Amount of entries in each directory
		//Retrieving first block number of root directory
		int firstBlockNo = (((bDir[24] & 0xFF) << 24)|((bDir[25] & 0xFF) << 16)|((bDir[26] & 0xFF) << 8)|(bDir[27] & 0xFF)); //Retrieve first block number of directory entries

		//Reading root directory items
		_tfs_read_block(firstBlockNo, tmp);

		while (true) {
			found = false; //Setting false variable to false
			//Iterate through
			for (int i = 0, secondI = 0; i < sizeOfDir; i++, secondI++){
				//If we read 4 entries already, we need to look at the following block that contains the rest of directory entries
				if (i%4 == 0){
					int nextBlock = fat.fatTable[entry];
					if (nextBlock == -1){
						return -1;
					}
					entry = nextBlock;
					_tfs_read_block(nextBlock, tmp);
					secondI = 0;
				}
				//Get the entry
				bDir = _tfs_get_bytes_block(tmp, (secondI*32), 32);

				//Compare the names
				for (int j = 8; j < 24; j++){
					n[j-8] = bDir[j];
				}
				String strName = new String(n);
				if (strName.equals(path[currName])){
					//If we are at the end of the path and we are at the last entry then return the parent block
					if (path.length-1 == currName){
						found = true;
						return parentBlockNo; //Retrieve parent block number of directory entry
					}
					found = true;
					currName++;
					sizeOfDir = (((bDir[28] & 0xFF) << 24)|((bDir[29] & 0xFF) << 16)|((bDir[30] & 0xFF) << 8)|(bDir[31] & 0xFF));
					sizeOfDir = sizeOfDir/BLOCK_SIZE;
					firstBlockNo = (((bDir[24] & 0xFF) << 24)|((bDir[25] & 0xFF) << 16)|((bDir[26] & 0xFF) << 8)|(bDir[27] & 0xFF)); //Retrieve first block number of directory entries
					parentBlockNo = firstBlockNo; //Updating parent block number
					_tfs_read_block(firstBlockNo, tmp); //Reading new block
					break; //Break out of first loop and look into next directory
				}
			}
			//If we arrive at the end of the loop, we hav
			if (found == false){
				return -1;
			}

		}

	}

	//_tfs_get_entry_dir method:
	//	Get the entry for name from the directory of which the first block number
	//	is block_no
	//	Returns -1 if name is not found, otherwise returns entry number in dir
	public static int _tfs_get_entry_dir(int block_no, byte[] name, byte nlength, byte[] is_directory, int[] fbn, int[] size){
		String strName = new String(name);
		//Creating buffer and reading block into it
		byte[] tmp = new byte[BLOCK_SIZE];
		byte[] n = new byte[16]; //name entry holder
		byte[] bDir = new byte[32]; //directory entry holder

		int count = 0;
		int entry = block_no;

		while (true){
			_tfs_read_block(entry, tmp);
			//Iterate through entries
			for (int i = 0; i < 4; i++){
				//Get entry
				bDir = _tfs_get_bytes_block(tmp, (i*32), 32);

				//Compare names
				for (int j = 8; j < 24; j++){
					n[j-8] = bDir[j];
				}
				String currName = new String(n);
				if (strName.equals(currName)){
					//Saving variables here
					nlength = bDir[5]; //Index 5 is nLength variable in Directory entry
					is_directory[0] = bDir[4]; //Index 4 is is_directory in Directory entry
					fbn[0] = (((bDir[24] & 0xFF) << 24)|((bDir[25] & 0xFF) << 16)|((bDir[26] & 0xFF) << 8)|(bDir[27] & 0xFF)); //Getting firstBlockNo as an int
					size[0] = (((bDir[28] & 0xFF) << 24)|((bDir[29] & 0xFF) << 16)|((bDir[30] & 0xFF) << 8)|(bDir[31] & 0xFF)); //Getting directory size as an int

					return count + i; //This is equal to entry number in directory
				}

			}
			int nextBlock = fat.fatTable[entry];
			if (nextBlock == -1){
				return -1;
			}
			entry = nextBlock;
			count++;
		}
	}

	//_tfs_create_entry_dir method:
	//	Creates an entry for name in the directory
	//	Returns -1 if spot is not available for entry in this directory
	public static int _tfs_create_entry_dir(int block_no, byte[] name, byte nlength, byte is_directory, int fbn, int size){

		//Creating new entry as an object
		Directory d = new Directory(name, nlength, is_directory, fbn, size);
		int entry = block_no;
		//int count = 0;
		boolean empty = true;

		byte[] tmp = new byte[BLOCK_SIZE];
		byte[] bDir = new byte[32];
		//Check to see if there is a spot in the directory
		while (true){
			_tfs_read_block(entry, tmp);
			//Iterate through entries
			for (int i = 0; i < 4; i++){
				empty = true; //Resets variable
				//Get entry
				bDir = _tfs_get_bytes_block(tmp, (i*32), 32);

				//If there is a byte that is not 0 then the entry is not empty
				for (int j = 0; j < 32; j++){
					if (bDir[j] != (byte)0){
						empty = false;
						break; //Leave loop
					}
				}
				//if we found an empty entry then we should write out directory entry there
				if (empty == true){
					//Allocate block number for directory
					if (fat.fatTable[fbn] != 0){
						System.out.println("The First Block Number (passed as parameter) of the directory entry being created is already being used.\nCreate entry with different first block number.");
					} else {
						fat.fatTable[fbn] = -1; //Initialize it as one block alone
						if (pcb.freeBlockPointer == fbn){
							int newFreeBlock = fat.findFreeBlock();
							if (newFreeBlock == -1){
								pcb.updateFreeBlockPointer(-1);
								return -1; //Return error because its full. No more free blocks.
							}
							pcb.updateFreeBlockPointer(newFreeBlock);
							tfs_sync(); //Sync from memory to disk
						}
					}
					_tfs_put_bytes_block(tmp, (i*32), d.dirBlock, 32); //Adding entry to block
					_tfs_write_block(entry, tmp); //Writing to disk
					return 0;
				}

			}
			int nextBlock = fat.fatTable[entry];
			if (nextBlock == -1){
				//Update FAT and PCB
				fat.fatTable[entry] = pcb.freeBlockPointer;
				fat.fatTable[pcb.freeBlockPointer] = -1;
				int freeBlock = fat.findFreeBlock(); //Gets new free block
				pcb.updateFreeBlockPointer(freeBlock);
				//Updating disk FAT and PCB
				tfs_sync();
			}
			entry = nextBlock;
			//count++;
		}

	}

	//_tfs_delete_entry method:
	//	Deletes the entry for name from the directory of which first block number
	//	is block_no
	public static int _tfs_delete_entry(int block_no, byte[] name, byte nlength){
		int entry = block_no;
		byte[] tmp = new byte[BLOCK_SIZE];
		byte[] bDir = new byte[32];
		byte[] n = new byte[16];

		String strName = new String(name); //Creating a String from byte[] name

		//Check for the file in directory
		while (true){
			_tfs_read_block(entry, tmp); //Reading block into buffer
			//Iterate through entries looking for name
			for (int i = 0; i < 4; i++){
				//Get directory entry
				bDir = _tfs_get_bytes_block(tmp, (i*32), 32);

				//Compare names
				for (int j = 8; j < 24; j++){
					n[j-8] = bDir[j]; //Writing name to name buffer
				}
				String currName = new String(n); //Converting byte[] to String
				//If we found the entry then delete the entry from directory
				if (strName.equals(currName)){
					//Setting all values of entry to 0
					for (int j = 0; j < 32; j++){
						bDir[j] = 0; //Set it to 0
					}

					_tfs_put_bytes_block(tmp, (i*32), bDir, 32); //Add changes to block
					_tfs_write_block(entry, tmp); //Write block to disk
					return 0;
				}
			}
			int nextBlock = fat.fatTable[entry];
			if (nextBlock == -1){
				return -1;
			}
			entry = nextBlock;
		}

	}

	//_tfs_update_entry_dir method:
	//	Update the entry for name in the directory of which the first block number
	//	is block_no
	public static int _tfs_update_entry_dir(int block_no, byte[] name, byte nlength, byte is_directory, int fbn, int size){
		int entry = block_no;
		byte[] tmp = new byte[BLOCK_SIZE];
		byte[] bDir = new byte[32];
		byte[] n = new byte[16];

		String strName = new String(name); //Creating a String from byte[] name

		//Find file and update it
		while (true) {
			_tfs_read_block(entry, tmp); //Reading block into buffer
			//Iterate through entries
			for (int i = 0; i < 4; i++){
				//Get directory entry
				bDir = _tfs_get_bytes_block(tmp, (i*32), 32);

				//Compare names
				for (int j = 8; j < 24; j++){
					n[j-8] = bDir[j]; //Writing name to name buffer
				}
				String currName = new String(n);
				//If we found the entry then update it
				if (strName.equals(currName)){
					bDir[4] = is_directory;
					bDir[5] = nlength;
					byte[] tmpInt = new byte[4];
					tmpInt[3] = (byte)fbn; tmpInt[2] = (byte)(fbn>>8); tmpInt[1] = (byte)(fbn>>16); tmpInt[0] = (byte)(fbn>>24);
					//Copying first block number
					for (int j = 24; j<28; j++){
						bDir[j] = tmpInt[j-24];
					}
					tmpInt[3] = (byte)size; tmpInt[2] = (byte)(size>>8); tmpInt[1] = (byte)(size>>16); tmpInt[0] = (byte)(size>>24);
					//Copying size
					for (int j = 28; j < 32; j++){
						bDir[j] = tmpInt[j-28];
					}

					_tfs_put_bytes_block(tmp, (i*32), bDir, 32);
					_tfs_write_block(entry, tmp);
					return 0;
				}
			}
			int nextBlock = fat.fatTable[entry];
			if (nextBlock == -1){
				return -1;
			}
			entry = nextBlock;
		}

	}

	//_tfs_read_bytes_fd method:
	//	Read up to length bytes from FileDescriptor starting at offset
	//	Returns number of bytes read
	private static int _tfs_read_bytes_fd(int fd, byte[] buf, int length)
	{
		FileDescriptor f = fdt.get(fd); //Create a reference to the FileDescriptor
		byte[] block = new byte[BLOCK_SIZE]; //This is where the bytes will be temporarily stored

		//Finding the right block that the filePointer points to
		int blockNo = _tfs_get_block_no_fd(fd, f.filePointer); //Finding location of block using file pointer as the offset
		_tfs_read_block(blockNo, block); //Read (copy) the block number found above

		//filePointer holds the offset to read from. We minus the amount of bytes in Blocks we skiped
		//This is done because we already retrieved the block equivalent to that offset just above
		buf = _tfs_get_bytes_block(block, f.filePointer - ((f.filePointer/BLOCK_SIZE)*BLOCK_SIZE), length); //Pass byte array into buffer using the block where filepointer points to, offset, and length of buffer

		return buf.length;
	}

	//_tfs_write_bytes_fd method:
	//	Write up to length bytes from buffer to file FileDescriptor points to
	//	Returns number of bytes written
	private static int _tfs_write_bytes_fd(int fd, byte[] buf, int length)
	{
		FileDescriptor f = fdt.get(fd); //Create a reference to the FileDescriptor

		//Finding the right block that the FilePointer points to
		int blockNo = _tfs_get_block_no_fd(fd, f.filePointer); //Finding location of block using file pointer as the offset
		int offset = f.filePointer - ((f.filePointer/BLOCK_SIZE)*BLOCK_SIZE); //		//filePointer holds the offset to read from. We minus the amount of bytes in Blocks we skiped

		//Holds block that will be modified
		byte[] block = new byte[BLOCK_SIZE];
		_tfs_read_block(blockNo, block); //Reading bytes into buffer

		//Overwriting bytes with new data
		_tfs_put_bytes_block(block, offset, buf, length);

		return length;
	}

	//_tfs_get_block_no_fd method:
	//	Block number for the offset in the file represented by fd (Check FAT table for correct block)
 	private static int _tfs_get_block_no_fd(int fd, int offset)
 	{
		FileDescriptor f = fdt.get(fd);
		int blockNo = f.startingBlock; //Initializing to where file is located
		int blockJumpCount = 0; //Number of blocks ahead of startingBlock (# of hops)
		if (offset > (BLOCK_SIZE-1)){ //BLOCK_SIZE-1 because offset is a list index from 0 to n-1
			blockJumpCount += (offset/BLOCK_SIZE);
		}

		//Retrieve the block that the file would be located at
		for (int i = 0; i < blockJumpCount; i++){
			blockNo = fat.fatTable[blockNo]; //Updates block number to next block
			//Check for two error cases
			if (blockNo == -1 || blockNo == 0){
				return -1; //Returns error
			}
		}

 		return blockNo; //If there is no error, return block number
 	}

	//_tfs_write_pcb method:
	//	Write PCB back into disk
	private static void _tfs_write_pcb(){
		//Write pcb at block 1 location
		_tfs_write_block(1, pcb.pcbBlock);
	}

	//_tfs_read_pcb method:
	//	Read PCB from disk into memory
	private static void _tfs_read_pcb(){
		byte[] pcbBuffer = new byte[BLOCK_SIZE]; //Creating and initializing buffer
		_tfs_read_block(1, pcbBuffer); //Reading block of bytes into buffer
		pcb.updatePCB(pcbBuffer); //Updates in memory pcb with disk pcb
	}

	//_tfs_write_fat method:
	//	Write FAT back into disk
	private static void _tfs_write_fat(){
		//Write fat starting at location 2
		for (int i = 2; i < fat.numBlocks+2; i++){
			_tfs_write_block(i, fat.fatBlocks[i-2]);
		}
	}

	//_tfs_read_fat method:
	//	Read FAT from the disk into memory
	private static void _tfs_read_fat(){
		byte[] fatBuffer = new byte[BLOCK_SIZE]; //Creating and initializing buffer
		for (int i = 2; i < fat.numBlocks+2; i++){
			_tfs_read_block(i, fatBuffer); //Reads block of FAT from disk
			fat.updateFATBlocks(fatBuffer, i-2); //Updates each block every iteration
		}
		//Update FAT Table entries
		fat.updateFATTable();
	}

	//_tfs_get_block_fat method
	//	Gets a free block from FAT
	private static int _tfs_get_block_fat(){
		return pcb.freeBlockPointer; //Returns index of free block in disk stored in PCB object
	}

	//_tfs_return_block_fat method:
	//	Returns a free block to File Allocation Table
	private static void _tfs_return_block_fat(int block_no){
		fat.fatTable[block_no] = 0; //0 means the block is free
		return;
	}

	//_tfs_attach_block_fat method:
	//	Attach new block to the end of the file (on FAT)
	private static int _tfs_attach_block_fat(int start_block_no, int new_block_no){
		int a = fat.fatTable[start_block_no];

		if (a == 0){
			return -1;
		}
		//Keep checking until we find -1 which means end of file
		while (fat.fatTable[a] != -1){
			a = fat.fatTable[a];
		}
		//Attach new block to the end of the file
		fat.fatTable[a] = new_block_no;
		fat.fatTable[new_block_no] = -1; //Set new block to point to end of file in fat

		return 0;
	}

	//===Block handling utilities===
	//_tfs_get_int_block method:
	//	Get an integer from a block
	private static int _tfs_get_int_block(byte[] block, int offset){
		byte[] tmp = new byte[4];
		int num;
		//Storing four bytes that form an int into tmp
		tmp[0] = block[offset]; tmp[1] = block[offset + 1]; tmp[2] = block[offset + 2]; tmp[3] = block[offset + 3];
		//Converting 4 bytes into int
		num = (((tmp[0] & 0xFF) << 24)|((tmp[1] & 0xFF) << 16)|((tmp[2] & 0xFF) << 8)|(tmp[3] & 0xFF));
		return num;
	}

	//_tfs_put_int_block method:
	//	Puts an integer into a block
	private static void _tfs_put_int_block(byte[] block, int offset, int data){
		//Translating data to byte array
		byte[] tmp = new byte[4];
		tmp[3] = (byte)data; tmp[2] = (byte)(data>>8); tmp[1] = (byte)(data>>16); tmp[0] = (byte)(data>>24);
		block[offset] = tmp[0]; block[offset+1] = tmp[1]; block[offset+2] = tmp[2]; block[offset+3] = tmp[3];
	}

	//_tfs_get_byte_block method:
	//	Get a byte from a block
	private static byte _tfs_get_byte_block(byte[] block, int offset){
		return block[offset];
	}

	//_tfs_put_byte_block method:
	//	Put a byte into a block
	private static void _tfs_put_byte_block(byte[] block, int offset, byte data){
		block[offset] = data;
	}

	//_tfs_get_bytes_block method:
	//	Get bytes from a block
	private static byte[] _tfs_get_bytes_block(byte[] block, int offset, int length){
		//Creating and initializing byte buffer
		byte[] buffer = new byte[length];
		for (int i = offset; i < (offset + length); i++){
			buffer[i - offset] = block[i]; //Copying bytes
		}
		return buffer;
	}

	//_tfs_put_bytes_block method:
	//	Put bytes into a block
	private static void _tfs_put_bytes_block(byte[] block, int offset, byte[] buf, int length){
		//Iterating through part of block and updating it
		for (int i = offset; i < (offset + length); i++){
			block[i] = buf[i-offset];
		}
	}

}


//PCB Class
// Keeps track of size of FAT, pointer to root directory
// and pointer to the next free block.
class PCB{
	//Creating class variables
	byte[] pcbBlock; //Byte array representes PCB block

	static int fatSize; //Size of FAT
	static int numFatBlocks;
	static int rootPointer; //Location of root
	static int freeBlockPointer; //Location of first free block

	//Object constructor
	PCB(int fatSize, int BLOCK_SIZE){
		this.fatSize = fatSize; //Size of fat table entries (int array)
		numFatBlocks = fatSize * 4; //*4 because an int is 4 bytes in java
		if (numFatBlocks % BLOCK_SIZE > 0){
			numFatBlocks = (numFatBlocks / BLOCK_SIZE) + 1;
		} else {
			numFatBlocks = numFatBlocks / BLOCK_SIZE;
		}

		rootPointer = 1 + 1 + numFatBlocks; //Block location where root is being initialized to (+1 +1 because of PCB and BCB)
		freeBlockPointer = 2 + numFatBlocks + 1; //0, 1, and FAT occupied blocks + 1 block after that will represent root directory

		pcbBlock = new byte[BLOCK_SIZE]; //Initialize PCB byte block array
		//Populating PCB byte block array
		byte tmp[] = new byte[4];
		//First 4 bytes of PCB represent size of FAT (number of entries)
		tmp[3] = (byte)fatSize; tmp[2] = (byte)(fatSize>>8); tmp[1] = (byte)(fatSize>>16); tmp[0] = (byte)(fatSize>>24);
		pcbBlock[0] = tmp[0]; pcbBlock[1] = tmp [1]; pcbBlock[2] = tmp[2]; pcbBlock[3] = tmp[3];
		//Second 4 bytes of PCB represent rootPointer;
		tmp[3] = (byte)rootPointer; tmp[2] = (byte)(rootPointer>>8); tmp[1] = (byte)(rootPointer>>16); tmp[0] = (byte)(rootPointer>>24);
		pcbBlock[4] = tmp[0]; pcbBlock[5] = tmp [1]; pcbBlock[6] = tmp[2]; pcbBlock[7] = tmp[3];
		//Third 4 bytes of PCB represent first free block
		tmp[3] = (byte)freeBlockPointer; tmp[2] = (byte)(freeBlockPointer>>8); tmp[1] = (byte)(freeBlockPointer>>16); tmp[0] = (byte)(freeBlockPointer>>24);
		pcbBlock[8] = tmp[0]; pcbBlock[9] = tmp [1]; pcbBlock[10] = tmp[2]; pcbBlock[11] = tmp[3];
	}

	//Receives in PCB block from disk and updates in memory object attributes
	public void updatePCB(byte[] pcbBuffer){
		//Updating root pointer from memory retrieved pcb buffer
		fatSize = (((pcbBuffer[0] & 0xFF) << 24)|((pcbBuffer[1] & 0xFF) << 16)|((pcbBuffer[2] & 0xFF) << 8)|(pcbBuffer[3] & 0xFF))*4/128;
		//Updating free block pointer from memory retrieved pcb buffer
		rootPointer = (((pcbBuffer[4] & 0xFF) << 24)|((pcbBuffer[5] & 0xFF) << 16)|((pcbBuffer[6] & 0xFF) << 8)|(pcbBuffer[7] & 0xFF));
		//Updating fat size from pointer from memory retrieved pcb buffer
		freeBlockPointer = (((pcbBuffer[8] & 0xFF) << 24)|((pcbBuffer[9] & 0xFF) << 16)|((pcbBuffer[10] & 0xFF) << 8)|(pcbBuffer[11] & 0xFF));
	}

	public void updateFreeBlockPointer(int pointer){
		freeBlockPointer = pointer;
		byte[] tmp = new byte[4];
		tmp[3] = (byte)freeBlockPointer; tmp[2] = (byte)(freeBlockPointer>>8); tmp[1] = (byte)(freeBlockPointer>>16); tmp[0] = (byte)(freeBlockPointer>>24);
		pcbBlock[8] = tmp[0]; pcbBlock[9] = tmp [1]; pcbBlock[10] = tmp[2]; pcbBlock[11] = tmp[3];
	}

}

//FAT Class
// Table from 0 to TotalBlockNumbers-1.
// The index of the block number contains the link to the next
// block. populateTest and populateBlock methods were created for
// testing purposes.
class FAT{
	//Creating class variables
	int BLOCK_SIZE;
	int fatSize;//number of entries
	int[] fatTable; //int array to represent table

	int numBlocks; //Size of fat in blocks of bytes
	byte[][] fatBlocks = null; //Array of bytes of n blocks. Each row is a block.

	//Object constructor
	FAT(int size, int BLOCK_SIZE){
			this.BLOCK_SIZE = BLOCK_SIZE;
			this.fatSize = size; //Setting size of FAT in entries
			fatTable = new int[fatSize]; //Initializing the file allocation table array

			numBlocks = size * 4; //*4 because an int is 4 bytes in java
			if (numBlocks % this.BLOCK_SIZE > 0){
				numBlocks = (numBlocks / this.BLOCK_SIZE) + 1;
			} else {
				numBlocks = numBlocks/this.BLOCK_SIZE;
			}
			// double tmp1 = ((double)fatSize*4.0)/(double)this.BLOCK_SIZE; //Number of blocks FAT will take up in DiskFile
			// int tmp = (int)Math.ceil(tmp1);

			fatBlocks = new byte[numBlocks][this.BLOCK_SIZE]; //Number of blocks needed to represent FAT in disk
	}

	//updateFATBlocks method:
	//	Updates the FAT blocks of bytes in memory using bytes from disk
	public void updateFATBlocks(byte[] fatBuffer, int k){
		for (int i = 0; i < fatBuffer.length; i++){
			//Copying bytes
			fatBlocks[k][i] = fatBuffer[i];
		}
	}

	//update fat from table to blocks
	public void updateBlocksFromTable(int entry, int value){

		int i = entry/32; //Gives i position in 2D block array
		int j = (entry-(32*i))*4; //times 4 because each int is 4 bytes

		byte[] tmp = new byte[4];
		tmp[3] = (byte)value; tmp[2] = (byte)(value>>8); tmp[1] = (byte)(value>>16); tmp[0] = (byte)(value>>24);
		//Updating blocks
		fatBlocks[i][j] = tmp[0]; fatBlocks[i][j+1] = tmp[1]; fatBlocks[i][j+2] = tmp[2]; fatBlocks[i][j+3] = tmp[3];
	}
	//updateFATBlocks method:
	//	Updates the FAT Table in memory using bytes from already updated from disk
	public void updateFATTable(){
		byte[] tmp = new byte[4];
		int num;
		int k = 0; //Index of fat entry we are updating
		//Iterating through FAT blocks and inserting them into in memory FAT table
		for (int i = 0; i < fatBlocks.length; i++){
			for (int j = 0; j < fatBlocks[i].length/4; j++){
				//Storing four bytes that form an int into tmp
				tmp[0] = fatBlocks[i][j*4]; tmp[1] = fatBlocks[i][(j*4)+1]; tmp[2] = fatBlocks[i][(j*4)+2]; tmp[3] = fatBlocks[i][(j*4)+3];
				//Converting 4 bytes into int
				num = (((tmp[0] & 0xFF) << 24)|((tmp[1] & 0xFF) << 16)|((tmp[2] & 0xFF) << 8)|(tmp[3] & 0xFF));
				fatTable[k] = num; //Updating FAT table entry
				k++; //Updating our index
			}
		}
	}

	//findFreeBlock method:
	//	Goes through in memory fat and finds free block - Starting at 68
	//	Returns -1 if no free blocks are available
	public int findFreeBlock(){
		for (int i = 68; i < fatTable.length; i++){
			if (fatTable[i] == 0){
				return i;
			}
		}
		return -1; //Returns -1 if no free blocks
	}
}

//Directory Class:
//	Implemented as a linked list
//	Each directory entry will take up 32 bytes (128/32 = 4 entries per block)
//	Structure of each entry (by index)(to is inclusive):
//
//		Byte 0 to 3 = parentBlockNo, byte 4 = isDirectory, byte 5 = nLength,
//		byte 6 to 7 = reserved, byte 8 to 23 = name, byte 24 to 27 = firstBlockNo,
//		byte 28 to 31 = size
//
class Directory {
	//Creating and initializing linked list
	//Each Directory object can have a lower level of directory. This will be
	// a list for each object.
	List<Directory> dList = new LinkedList<Directory>();


	//Creating byte array block to store directory in disk
	byte[] dirBlock = new byte[32];

	//-------------------- These should be once only --------------------
	//int noEntries; //Total number of entries
	int parentBlockNo; //The first block number of the parent dir

	byte isDirectory; //0: subdirectory, 1: file
	byte nLength; //name length
	byte reserved1;
	byte reserved2;
	byte[] name = new byte[16]; //not a full path
	int firstBlockNo; // The first block number
	int size; // The size of the file or subdirectory


	//Object constructor
	Directory(byte[] name, byte nlength, byte is_directory, int fbn, int size){
		this.name = name;
		this.nLength = nlength;
		this.isDirectory = is_directory;
		this.firstBlockNo = fbn;
		this.size = size; //This is in bytes - Each entry is 32 bytes long
		//Initialize dirBlock
		this.createDirBlock();
	}

	//Used when appending subdirectories to parent directories
	public void attach(Directory d){
		dList.add(d); //Add Directory object to the list
	}

	public void createDirBlock(){
		//Translating ints to byte
		byte[] pbn = new byte[4];
		pbn[3] = (byte)parentBlockNo; pbn[2] = (byte)(parentBlockNo>>8); pbn[1] = (byte)(parentBlockNo>>16); pbn[0] = (byte)(parentBlockNo>>24);
		byte[] fbn = new byte[4];
		fbn[3] = (byte)firstBlockNo; fbn[2] = (byte)(firstBlockNo>>8); fbn[1] = (byte)(firstBlockNo>>16); fbn[0] = (byte)(firstBlockNo>>24);
		byte[] s = new byte[4];
		s[3] = (byte)size; s[2] = (byte)(size>>8); s[1] = (byte)(size>>16); s[0] = (byte)(size>>24);


		//Populating directory block to be written to disk
		for (int i = 0; i < 4; i++){
			dirBlock[i] = pbn[i];
		}
		dirBlock[4] = isDirectory;
		dirBlock[5] = nLength;
		dirBlock[6] = reserved1;
		dirBlock[7] = reserved2;
		for (int i = 8; i < 24; i++){
			dirBlock[i] = name[i-8];
		}
		for (int i = 24; i < 28; i++){
			dirBlock[i] = fbn[i-24];
		}
		for (int i = 28; i < 32; i++){
			dirBlock[i] = s[i-28];
		}
	}
}

//File Descriptor Class
//	Implemented as a linked list
class FileDescriptor{
	//Class variables
	byte[] name;
	byte isDirectory;
	int startingBlock;
	int filePointer; //This is the offset where the process reads from or writes to
	int fileSize; //Total size in bytes

	FileDescriptor (byte name[], int nlength, byte is_directory, int first_block_no, int file_size){
		this.name = name;
		this.isDirectory = is_directory;
		this.startingBlock = first_block_no;
		this.fileSize = file_size;
	}

	FileDescriptor (byte name[], byte isDirectory, int startingBlock, int filePointer, int fileSize){
		this.name = name;
		this.isDirectory = isDirectory;
		this.startingBlock = startingBlock;
		this.filePointer = filePointer;
		this.fileSize = fileSize;
	}

}
