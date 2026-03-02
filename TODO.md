## To-Do:

Start Page
- Remove (?) browse Eloc status

Status page: 
- Show remaining recording time (calculating from Sample rate and SD space available. See example below)
- Reorganize shown data

Settings page: 
- Make it more user friendly



The formula to calculate the size of a WAV file is:

Size (in bytes) = sample rate x bit depth x number of channels x duration

First, let's convert the sample rate to bytes per second:

16 KHz = 16,000 samples per second
16 bits = 2 bytes per sample (since 1 byte = 8 bits)
1 channel = mono

So, the bytes per second would be:

16,000 x 2 x 1 = 32,000 bytes per second

To find out how many bytes are required for one minute of recording, we need to multiply the bytes per second by 60:

32,000 x 60 = 1,920,000 bytes per minute

Since 1 MB = 1,048,576 bytes, we can divide the bytes per minute by this number to get the size in megabytes:

1,920,000 / 1,048,576 = 1.83 MB (approximately)

Therefore, one minute of mono recording in WAV format with a sample rate of 16 KHz and 16-bit depth would require approximately 1.83 MB of storage.
