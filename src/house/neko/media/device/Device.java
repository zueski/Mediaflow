package house.neko.media.device;

import house.neko.media.common.Media;
import house.neko.media.common.MediaLocation;

import java.io.File;
import java.io.IOException;

public interface Device
{
	public File copyTo(Media m, boolean overWriteExisting) throws Exception;
	
	public File copyTo(Media m) throws Exception;
	
	public File getDeviceFile(Media m, MediaLocation l) throws IOException;
	
	public void deleteAll() throws IOException;
	
	public long getFreeSpace();
	
	public long getAvailableSpaceForTracks();
}