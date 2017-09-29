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

		//  tru._tfs_write_pcb();
		//  tru._tfs_read_pcb();
		//  tru._tfs_read_fat();

		//System.out.println("PCB SAYS FAT BLOCKS ARE: "+pcb.numFatBlocks + "\nFAT SAYS FAT BLOCKS ARE: "+ fat.numBlocks);
		//  //Reading bytes from memory
		//  byte[] test = new byte[BLOCK_SIZE];
		//  for (int i = 2; i < fat.numBlocks+2; i++){
		// 	 disk.tfs_dio_read_block(i, test);
		// 	 System.out.print("BLOCK "+i + ": ");
		// 	 for (int j = 0; j < BLOCK_SIZE; j++){
		// 		 System.out.print(test[j]);
		// 	 }
		// 	 System.out.println();
		//  }

		//  //TEST reading from Disk
		//  //Try reading FAT TABLE from file
		//  byte[] tester = new byte[fat.fatBlocks.length];
		//  disk.tfs_dio_read_block(2, tester);
		//  System.out.println("\n\n\n\n\nTHIS IS THE DATA IN BYTES:");
		//  int tmp = 0;
		//  for (int i = 0; i < tester.length; i++, tmp++){
		// 	 if (tmp == 4){
		// 		 System.out.print("\t");
		// 		 tmp = 0;
		// 	 }
		// 	 System.out.print(tester[i]);
		//  }
		//  //READING DATA FROM BYTES TO integers
		//  System.out.println("\n\n\n\n\nTHIS IS THE DATA IN INTEGERS:");
		//  tmp = 0;
		//  int result;
		//  for (int i = 0; i < tester.length; i++, tmp++){
		// 	 result = (((tester[i]& 0xFF) << 24)|((tester[i+1] & 0xFF) << 16)|((tester[i+2] & 0xFF) << 8)|(tester[i+3] & 0xFF));
		// 	 System.out.print(result + "\t");
		// 	 result = 0;
		// 	 i += 3;
		// 	 System.out.println("I IS EQUAL TO: "+ i);
		//  }
		//
		// //  Reading PCB
		// System.out.println("\n\nPCB:");
		//  byte[] read = new byte[BLOCK_SIZE];
		//  disk.tfs_dio_read_block(1, read);
		//  for (int i = 0; i < read.length; i++){
		// 	 System.out.print(read[i]);
		//  }
		//  System.out.println();
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
		root = new Directory("/");

		// //Testing purposes:
		//---------- TEST FAT
		// fat.populateTest();
		// fat.populateBlocks();
		//System.out.println(tfs_prrfs());
		//----------

		//Writing PCB and FAT from memory to disk
		_tfs_write_pcb();

		//Write FAT starting at block 2
		//disk.tfs_dio_write_block(2, fat.fatBlocks); //Writing FAT blocks of bytes to disk
		for (int i = 2; i < fat.numBlocks+2; i++){
			_tfs_write_block(i, fat.fatBlocks[i-2]);
		}

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

	public static int tfs_sync()
	{
		return -1;
	}

	//tfs_prrfs method:
	// Loads PCB and FAT from memory into a buffer.
	// Returns both in a string.
	public static String tfs_prrfs()
	{
		String blocksInDisk = null;

		byte[] pcbBuffer = new byte[BLOCK_SIZE]; //Buffer for pcb block - PCB is one block in length
		byte[] fatBuffer = new byte[BLOCK_SIZE]; //Buffer for fat of length of fat

		// disk.tfs_dio_read_block(1, pcbBuffer); //Reading pcb block into Buffer
		// disk.tfs_dio_read_block(2, fatBuffer); //Reading FAT blocks into buffer
		_tfs_read_block(1, pcbBuffer);
		//_tfs_read_block(2, fatBuffer);

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

	public static int tfs_open(byte[] name, int nlength)
	{
		return -1;
	}

	public static int tfs_read(int file_id, byte[] buf, int blength)
	{
		return -1;
	}

	public static int tfs_write(int file_id, byte[] buf, int blength)
	{
		return -1;
	}

	public static int tfs_seek(int file_id, int position)
	{
		return -1;
	}

	public static void tfs_close(int file_id)
	{
		return;
	}

	public static int tfs_create(byte[] name, int nlength)
	{
		return -1;
	}

	public static int tfs_delete(byte[] name, int nlength)
	{
		return -1;
	}

	public static int tfs_create_dir(byte[] name, int nlength)
	{
		return -1;
	}

	public static int tfs_delete_dir(byte[] name, int nlength)
	{
		return -1;
	}


	/*
	 * TFS private methods to handle in-memory structures
	 */

 	private static int _tfs_read_block(int block_no, byte buf[])
 	{
		//Calling TFSDiskInputOuput read method
		int response = disk.tfs_dio_read_block(block_no, buf);
 		return response; //Returning reponse from method
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
 	private static int _tfs_open_fd(byte name[], int nlength, int first_block_no, int file_size)
 	{
		//Creating File Descriptor object
		FileDescriptor fd = new FileDescriptor(name, nlength, first_block_no, file_size);
		fdt.add(fd); //Adding it to the File Descriptor Table (Implemented as a linked list)
 		return (fdt.size() - 1); //Returning index of file descriptor
 	}

	//_tfs_seek_fd method:
	//	Change the file pointer to offset
	//	NOTE: offset variable is based on indexes. These indexes go from 0 to n-1.
 	private static int _tfs_seek_fd(int fd, int offset)
 	{
		//If offset is not valid, return error
		if (offset < 0){
			return -1;
		}
		fdt.get(fd).filePointer = offset;
 		return 0;
 	}

	//_tfs_close_fd method:
	//	Remove FileDescriptor of index fd from File Descriptor Table
 	private static void _tfs_close_fd(int fd)
 	{
		fdt.remove(fd); //Removes FileDescriptor object from FDT
 		return;
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
		return 0;
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

	private static void _tfs_return_block_fat(int block_no){

	}
	private static int _tfs_attach_block_fat(int start_block_no, int new_block_no){
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
//Switched implementation from 1D array to 2D array. These methods don't work anymore
// 	//Populate fatTable method - Testing purposes
// 	public void populateTest(){
// 			for (int i = 0; i < fatTable.length; i++){
// 				fatTable[i] = 21696;
// 			}
// 	}
// 	//Create FAT Blocks method - Testing purposes
// 	public void populateBlocks(){
// 		int a;
// 		byte[] b = new byte[4];
// 		int index = 0;
//
// 		for (int i = 0; i < fatTable.length; i++){
// 			a = fatTable[i];
// 			b[3] = (byte)a;
// 			b[2] = (byte)(a >> 8);
// 			b[1] = (byte)(a >> 16);
// 			b[0] = (byte)(a >> 24);
//
// 			for (int j = 0; j < b.length; j++){
// 				//fatBlocks[(i*4) + j] = b[j];
// 			}
// 		}
// 	}
}

//Directory Class:
//	Implemented as a linked list
class Directory {
	//Creating and initializing linked list
	List<String> list = new LinkedList<String>();
	//Creating bytes blocks to store directory in disk
	byte[] directoryBlocks;

	//Object constructor
	Directory(String name){
		list.add(name); //Adds passed in String to list
	}
	//Used when appending subdirectories to parent directories
	public void append(List<String> l){
		list.addAll(l);
	}
}

//File Descriptor Class
//	Implemented as a linked list
class FileDescriptor{
	//Class variables
	byte[] name;
	boolean isDirectory;
	int startingBlock;
	int filePointer; //This is the offset where the process reads from or writes to
	int fileSize; //Total size in bytes

	FileDescriptor (byte name[], int nlength, int first_block_no, int file_size){
		this.name = name;
		this.startingBlock = first_block_no;
		this.fileSize = file_size;
	}

	FileDescriptor (byte name[], boolean isDirectory, int startingBlock, int filePointer, int fileSize){
		this.name = name;
		this.isDirectory = isDirectory;
		this.startingBlock = startingBlock;
		this.filePointer = filePointer;
		this.fileSize = fileSize;
	}

}
