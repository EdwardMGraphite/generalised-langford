package digitSequence;

import java.util.Date;

public class A322178 {

	private static long[] work;
	private static final boolean stronglyDecreasing = true; //Set true to calculate https://oeis.org/A060963
															//false to calculate https://oeis.org/A322178
	private static final int N = 12;
	private static final int length = N * 2;
	private static final long[] powers = new long[length];
	private static final LongLongMap2 memo = new LongLongMap2(length + 5, //key length of the cache, in bits
																length > 25 ? 28 : length + 4, //hashLength in bits
																6, //multiplier, storage capacity of the cache = multiplier * 2^hashLength
																	//A multiplier of 6 is sufficient for n = 15 (A060963) and n = 14 (A322178)
																	//A multiplier of 7 is required for the next values
																36, //Bits available for storing values in the cache, larger values will not be stored,
																	//but since there will be relatively few of these, performance will not be unduly affected
																12);
	private static long LEFT_BITMASK;
	private static Date startDate;
	
	public static void main(String[] args) {

		System.out.println(Runtime.getRuntime().maxMemory());
		
		powers[0] = 1;
		for (int i = 1; i < length; i++)
			powers[i] = 2 * powers[i-1];
		
		LEFT_BITMASK = powers[length - 1];
			
		work = new long[N + 1];
		
		int[] digits = new int[length];
		startDate = new Date();
		System.out.println(startDate);
		System.out.println(recurse(digits, N, 0, 0, length - 2) + " solutions") ;
		long totalWork = 0;
		for (int i=0; i < N + 1; i++ ){
			System.out.println(i + ": work = " + work[i]);
			if (i > 0)
				totalWork += work[i];
		}
		
		System.out.println(totalWork + " total work");
		System.out.println(memo.size() + " entries in cache");
		
		System.out.println(new Date());
		
	}
	
	private static long recurse(int[] digits, int n, long used, long mirrorUsed, int maxNumberAvailable){
		work[n] = work[n] + 1;
		if (n == 0){
//			StringBuilder solution = new StringBuilder();
//			for (int i = 0 ; i < length ; i++)
//				solution.append(digits[i]).append(" ");
//			System.out.println(solution.toString());
			return 1;
		}
		
		long cacheValue;
		long shiftedUsed = used;
		while ((shiftedUsed & LEFT_BITMASK) != 0)
			shiftedUsed = ((shiftedUsed ^ LEFT_BITMASK) << 1) + 1;
		shiftedUsed |= ((long) maxNumberAvailable) << length;

		cacheValue = memo.get(shiftedUsed);
		if (cacheValue != -1)
			return cacheValue;
		long shiftedMirrorUsed = mirrorUsed;
		while ((shiftedMirrorUsed & LEFT_BITMASK) != 0)
			shiftedMirrorUsed = ((shiftedMirrorUsed ^ LEFT_BITMASK) << 1) + 1;
		shiftedMirrorUsed |= ((long) maxNumberAvailable) << length;
		cacheValue = memo.get(shiftedMirrorUsed);
		if (cacheValue != -1)
			return cacheValue;
		
		long counter = 0;
		if (maxNumberAvailable != 0) {
			int gaps = 0;
			int filled = 0;
			int compulsory = 0;
			for (int i = 0; i < length; i++){
				if (digits[i] == 0){
					if (filled > maxNumberAvailable && gaps % 2 == 1){
						memo.put(shiftedUsed, 0);
						return 0;
					}
					else if (stronglyDecreasing && filled == maxNumberAvailable && gaps % 2 == 1){
						if (compulsory != 0){
							memo.put(shiftedUsed, 0);
							return 0;
						}
						compulsory = i;
					}
					gaps++;
					filled = 0;
				}
				else 
					filled++;
			}
			
			if (compulsory != 0 ){
				digits[compulsory] = n;
				digits[compulsory - maxNumberAvailable - 1] = n;
				counter = recurse(digits, n - 1, n > 1 ? used + powers[compulsory] + powers[compulsory - maxNumberAvailable - 1]: used,
													n > 1 ? mirrorUsed + powers[length - compulsory - 1] + powers[length - compulsory + maxNumberAvailable]: mirrorUsed, maxNumberAvailable - 1);
				digits[compulsory] = 0;
				digits[compulsory - maxNumberAvailable - 1] = 0;
				memo.put(shiftedUsed, counter);
				return counter;
			}
		}
		for (int num = maxNumberAvailable; num >= (stronglyDecreasing ? n - 1 : 0); num--) {
			for (int i = 0; i < length - num - 1; i++){
				if (digits[i] == 0 && digits[i + num + 1] == 0){
					digits[i] = n;
					digits[i + num + 1] = n;
					counter += recurse(digits, n - 1, n > 1 ? used + powers[i] + powers[i + num + 1]: used,
							n > 1 ? mirrorUsed + powers[length - 1 - i] + powers[digits.length - 2 - i - num]: mirrorUsed, num - (stronglyDecreasing ? 1 : 0));
					if (n == N){
						Date now = new Date();
						System.out.println("Num= " + num + ", i= " + i + ", " + memo.size() + " in cache, " + counter + " solutions, NSF=" + memo.getNoSpaceFound() + ", VTL=" + memo.getValueTooLarge() + ", MPS=" + memo.getMaxPointerSearch() + ", " + ((now.getTime() - startDate.getTime())/ 1000.0) + " s" );
					}
					digits[i] = 0;
					digits[i + num + 1] = 0;
				}
			}
		}
		
		memo.put(shiftedUsed, counter);
		return counter;
		
	}

}
