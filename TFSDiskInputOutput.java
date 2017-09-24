import java.io.*;
import java.util.*;

public class TFSDiskInputOutput
{
	/*
	 * Disk I/O API
	 */

	 //NOTES: block size = 128bytes
	 //	-Add if (buf.length < blocksize: return -1) Only do this after testing is done with actual blocks

	 public static final int BLOCK_SIZE = 128;
	 //Class variables:
	 static RandomAccessFile raf = null; //RandomAccessFile object that class methods will interact with
	 static int numBlocks = 0; //This tracker variable is updated when tfs_dio_get_size is called

	 //For testing purposes only
	 public static void main (String args[]){

		 byte[] test = {'h', 'e', 'l', 'l', 'o'};
		 tfs_dio_create(test, test.length, 2048);
		 tfs_dio_open(test, test.length);
		 tfs_dio_get_size();
		 System.out.println("Number of blocks: " + numBlocks);

		 //Writing and reading test
		 tfs_dio_write_block(2, test);
		 byte[] arr = new byte[test.length];
		 tfs_dio_read_block(2, arr);
		 System.out.println(new String(arr));

		 tfs_dio_close();
	 }

	 //tfs_dio_create method:
	 // Attempts to create a disk file and catches exception.
	public static int tfs_dio_create(byte[] name, int nlength, int size)
	{
		//Note: Will not overwrite file if already created
		//Creating file
		File f = null;
		raf = null;

		try {
			//Creating and initializing file
			f = new File(new String(name));
			//Check to see if it exists and create file, otherwise return error
			if (!f.exists()){
				f.createNewFile();
			} else{
				return -1; //Return error
			}

			raf = new RandomAccessFile(f, "rw");
			//Set the length of the file now
			raf.setLength(size * BLOCK_SIZE); //size = blocks and 128bytes is the size each block

		} catch(IOException ioe){
			System.out.println("IOException error: " + ioe.getMessage());
			return -1; //Return error
		} finally{
			if (raf != null){
				try{raf.close();}
				catch(IOException ioe){
					System.out.println("There was an error closing the file: " + ioe.getMessage());
				}
			}
		}
		raf = null; //Set RandomAccessFile object back to null
		return 0; //Return no error
	}

	//tfs_dio_open method:
	// Opens the disk file.
	public static int tfs_dio_open(byte[] name, int nlength)
	{
		//Try opening file and catch exception
		try{
			File f = new File(new String(name)); //Creating File object
			raf = new RandomAccessFile(f, "rw"); //Initializing RAF object
		} catch (IOException ioe){
			System.out.println("There was an error opening the file: " + ioe.getMessage());
			return -1;
		}

		return 0;
	}

	//tfs_dio_get_size method:
	// Returns the size of the disk file
	public static int tfs_dio_get_size()
	{
		//Blocks are 128bytes long
		//Try to get size of file and catch exception
		try{
			numBlocks = (int)(raf.length() / BLOCK_SIZE); //Length in bytes divided by length of each block. Also convertin to int as we know result will be a whole number
		} catch (Exception e){
			System.out.println("There was an error calculating the size of the file: " + e.getMessage());
			return -1;
		}
		return 0;
	}

	//tfs_dio_read_block method:
	// Reads the a block of bytes in disk file into byte buffer that is passed as parameter.
	// Returns -1 if there is an error.
	public static int tfs_dio_read_block(int block_no, byte[] buf)
	{
		//Try to read from disk and catch exceptions
		try {
			int pos = BLOCK_SIZE * block_no; //Get byte position to read from
			raf.seek(pos); //Update pointer position
			raf.read(buf, 0, BLOCK_SIZE); //Read bytes available into byte array buffer
		} catch(IOException ioe){
			System.out.println("There was an error reading from disk: " + ioe.getMessage());
			return -1;
		} catch (NullPointerException npe){
			System.out.println("There was an error reading from disk: " + npe.getMessage());
			return -1;
		}
		return 0;
	}

	//tfs_dio_write_block method:
	// Writes block of bytes in disk file from byte buffer that is passed as a parameter.
	// Returns -1 if there is an error.
	public static int tfs_dio_write_block(int block_no, byte[] buf)
	{
		//Try to write to disk and catch exception
		try{
			int pos = BLOCK_SIZE * block_no; //Get byte position to write to
			raf.seek(pos); //Update pointer position
			raf.write(buf, 0, BLOCK_SIZE); //Write bytes from byte array to pointer curr position
		} catch(IOException ioe){
			System.out.println("There was an error writing to the disk: " + ioe.getMessage());
			return -1;
		}
		return 0;
	}

	//tfs_dio_close method:
	// Attemps to close the file.
	public static void tfs_dio_close()
	{
		//Try closing the file, catch exception
		if (raf != null){
			try {
				raf.close();
			} catch(IOException ioe){ //Catches NullPointerException + IOException
				System.out.println("There was an error closing the file: " + ioe.getMessage());
			}
		}
		raf = null;
	}
}
