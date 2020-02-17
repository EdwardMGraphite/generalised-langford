package digitSequence;

/**
 * Storage capacity is multiplier * 2^hashLength
 * 
 *
 */
public class LongLongMap2 {

	private final int hashLength;
	private final long[] array;
	private final int recordLength;
	private final int multiplier;
	private final int valueBits;
	private final int pointerBits;
	private final int maxPointer;
	private final int keyLength;
	private final long maxValue;
	private int size = 0;
	private final int storage;
	private boolean full;
	private long noSpaceFound = 0;
	private long valueTooLarge = 0;
	private int maxPointerSearch = 0;
	private int maxPointerSearchAddress = -1;
	private int pointerSearches = 0;
	private int pointerSearchLength = 0;
	
	public LongLongMap2(int keyLength, int hashLength, int multiplier, int valueBits, int pointerBits) {
		storage = (int) (Math.pow(2, hashLength) * multiplier);
		System.out.println("Cache size: " + storage);
		this.hashLength = hashLength;
		this.multiplier = multiplier;
		this.valueBits = valueBits;
		this.pointerBits = pointerBits;
		this.keyLength = keyLength;
		maxPointer = (int) (Math.pow(2, pointerBits));
		maxValue = (long) (Math.pow(2, valueBits) - 1);
		recordLength = 1 + (keyLength - hashLength) + pointerBits + valueBits;
		array = new long[(int) (1 + (((long) storage) * recordLength / 64))];
	}
	
	private long getField(long address, int bitCount, int bitStart) {
		
		int index = (int) ((address * recordLength + bitStart) / 64);
		long offset = (address * recordLength + bitStart) % 64;

		long result = (array[index] << offset) >>> (64 - bitCount);  

		if (offset + bitCount > 64){
			index = index + 1;
			result |= array[index] >>> (64 - (offset + bitCount - 64));
		}
		return result;
		
	}
	
	private boolean isRecordUsed(long address) {
		return getField(address, 1, 0) == 1;
	}

	private long getKeyBits(long address) {
		return getField(address, keyLength - hashLength, 1);
	}
	
	private int getPointer(long address) {
		return (int) getField(address, pointerBits, 1 + (keyLength - hashLength));
	}
	
	private long getValue(long address) {
		return getField(address, valueBits, recordLength - valueBits);
	}
	
	private void writeField(long address, int bitCount, int bitStart, long value) {
		
		int index = (int) ((address * recordLength + bitStart) / 64);
		long offset = (address * recordLength + bitStart) % 64;
		
		if (offset + bitCount < 64 ) {
			long newEntry = array[index] >>> (64 - offset) << (64 - offset);
			newEntry |= array[index] << (offset + bitCount) >>> (offset + bitCount);
			newEntry |= value << (64 - offset - bitCount);
			array[index] = newEntry;
		}
		else if (offset + bitCount > 64 ){
			long newEntry = array[index] >>> (64 - offset) << (64 - offset);
			newEntry |= value >>> (offset + bitCount - 64);
			array[index] = newEntry;
			newEntry = array[index + 1] << (offset + bitCount - 64) >>> (offset + bitCount - 64); 
			newEntry |= value << (64 - (offset + bitCount - 64));
			array[index + 1] = newEntry;
		}
		else {
			array[index] = (array[index] >>> bitCount) << bitCount;
			array[index] |= value;
		}
		
	}
	
	private void setRecordUsed(long address, boolean used) {
		writeField(address, 1, 0, used ? 1 : 0);
	}
	private void setKey(long address, long key) {
		writeField(address, keyLength - hashLength, 1, key);
	}
	private void setPointer(long address, int pointer) {
		writeField(address, pointerBits,  1 + (keyLength - hashLength), pointer);
	}
	private boolean setValue(long address, long value) {
		if (value > maxValue) {
			valueTooLarge++;
			return false;
		}
		else {
			writeField(address, valueBits,  recordLength - valueBits, value);
			return true;
		}
	}
	
	public long get(long key){
		
		int address = multiplier * getHash(key);
		
		key = key >>> hashLength;
		
		while (isRecordUsed(address)){
		
			long trialKey = getKeyBits(address);
			
			if (trialKey == key)
				return getValue(address);
			
			int pointer = getPointer(address);
			
			if (pointer > 0)
				address = getPointerAddress(address, pointer);
			else
				break;
			
		}
		
		return -1;
	}
	
	public void put(long key, long value){
		
		if (full)
			return;
		
		int address = multiplier * getHash(key);
		
		key = key >>> hashLength;
		int pointer = 0;
		int oldAddress = -1;
		
		while (true) { 
			if (!isRecordUsed(address)){
				if	(setValue(address, value)) {
					size++;
					if (size == storage)
						full = true;
					setRecordUsed(address, true);
					setKey(address, key);
					if (pointer > 0 && oldAddress >= 0)
						setPointer(oldAddress, pointer);
				}
				return;
			}
			
			long trialKey = getKeyBits(address);
			
			if (trialKey == key){
				setValue(address, value);
				return;
			}

			pointer = getPointer(address);
			if (pointer != 0) {
				address = getPointerAddress(address, pointer);
				continue;
			}
//			pointerSearches++;
			for (int i = 1; i < maxPointer; i++) {
//				pointerSearchLength++;
				if (i > maxPointerSearch) {
					maxPointerSearch = i;
//					maxPointerSearchAddress = address;
				}
				if (getPointerAddress(address, i) % multiplier == 0)
					continue;
				if (!isRecordUsed(getPointerAddress(address, i))) {
					oldAddress = address;
					address = getPointerAddress(address, i);
					pointer = i;
					break;
				}
			}
			if (oldAddress >= 0)
				continue;
			noSpaceFound++;
			return;
		}
		
	}
	public void reset() {
		for (int i = 0; i < array.length; i++)
			array[i] = 0;
		full = false;
		noSpaceFound = 0;
		valueTooLarge = 0;
		maxPointerSearch = 0;
		maxPointerSearchAddress = -1;
		pointerSearches = 0;
		pointerSearchLength = 0;
	}
	
	private int getHash(long key) {
		return (((int)(key ^(key >>> 32))) << (32 - hashLength)) >>> (32 - hashLength);
	}
	
	private int getPointerAddress(int address, int pointer) {
		int result = ((address + (pointer * pointer * pointer)) % storage);
		if (result < 0)
			result = -result;
		return result;
	}
	
	public int size(){
		return size;
	}

	public long getNoSpaceFound() {
		return noSpaceFound;
	}

	public long getValueTooLarge() {
		return valueTooLarge;
	}

	public int getMaxPointerSearch() {
		return maxPointerSearch;
	}
	
	public int getMaxPointerSearchAddress() {
		return maxPointerSearchAddress;
	}

	public int[] getHistogram(int binSize) {
		int[] result = new int[1 + storage / binSize];
		
		for (int i = 0; i < storage; i++)
			if ((i % multiplier) != 0 && isRecordUsed(i))
				result[i / binSize]++;
		
		return result;
	}

	public int getPointerSearches() {
		return pointerSearches;
	}

	public int getPointerSearchLength() {
		return pointerSearchLength;
	}

	public static class Iterator {
	
		private final LongLongMap2 map;
		private long baseAddress;
		private long address = 0;
		private int pointer = 0;
		private boolean hasNext = false;
		
		public Iterator(LongLongMap2 map) {
			this.map = map;
			baseAddress = -map.multiplier;
			findNext();
		}
		
		public long next() {
			long oldAddress = address;
			findNext();
			return oldAddress;
		}
		
		public boolean hasNext() {
			return hasNext;
		}

		private void findNext() {
			
			while (pointer != 0) {
				address = map.getPointerAddress((int) address, pointer);
				pointer = map.getPointer(address);
				if (map.isRecordUsed(address))
					return;
			}
			
			hasNext = false;
			for (long i = baseAddress + map.multiplier; i < map.storage/map.multiplier; i += map.multiplier) {
				if (map.isRecordUsed(i)) {
					address = i;
					baseAddress = address;
					pointer = map.getPointer(address);
					hasNext = true;
					break;
				}
			}
			if (!hasNext)
				address = -1;
		}

	}
}
