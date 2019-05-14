import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
class Dict{
    class Cache{
        int d_flag;
        int d_offset;
        int d_count;
    };
    class IntBuff{
        int d_byteCount;
        byte [] d_buff;
        IntBuff(int size){
            d_buff = null;
            d_byteCount = 0;
            d_buff = new byte [size];
        }
        IntBuff(){
            d_byteCount = 0;
            d_buff = null;
        }
        byte [] getBuff(){
            return d_buff;
        }
        void storeInt(int i){
           if (d_buff != null){
           d_buff[d_byteCount+3] = (byte)((i)  & 0xff);
           d_buff[d_byteCount+2] = (byte)((i >> 8)  & 0xff);
           d_buff[d_byteCount+1] = (byte)((i >> 16) & 0xff);
           d_buff[d_byteCount] = (byte)((i >> 24) & 0xff);
           d_byteCount += 4;
           }
        }
        int getByte(){
            return d_byteCount;
        }
    };
    FileInputStream d_isIndex = null;
    FileInputStream d_isDict = null;
    FileInputStream d_isCache = null;
    


    int [] d_offsetArray; // store evevy index word offset
    int d_count;  // current indexFile word index

    Cache[][] d_indexCache; // [i][j] 

    String d_word; // current word;
    int d_ip;  // current word index
    boolean d_over;  // indexfile end flag
    byte [] d_buff;  // indexFile buff
    byte [] d_wordBuff; 

    int d_buffBegin; // file's offset at buffBegin
    int d_offset; //  indexFile offset

    int d_buffEnd;

    int d_explainOffset; 
    int d_explainSize;
    final int BUFFSIZE = 4096;
    final int CACHESIZE = 128;
    String d_dictFileName=null;

    Dict(String filename) 
    {
        d_dictFileName = filename;
        if (!init())
            return;
        initCache();
    }
    boolean init(){
        try{
	    if (d_isIndex != null)
		    d_isIndex.close();
	    if (d_isDict != null)
		    d_isIndex.close();

            d_isIndex = new FileInputStream(d_dictFileName +".idx");
            d_isDict  = new FileInputStream(d_dictFileName +".dict");

            d_buffBegin = 0;
            d_offset=0; 
            d_buffEnd = BUFFSIZE;
            d_buff = new byte[BUFFSIZE];
            d_ip = 0;
            d_over = false;
            d_count = 0;
            return true;
        }catch(Exception e){
            System.out.println(e.toString());
            return false;
        }
    }
    void moveBuff(){
        for(int i = d_ip,j=0; i < d_buffEnd; i++,j++){
            d_buff[j] = d_buff[i];
        } 
        d_buffBegin += d_ip;
    }
    boolean readIndexBuff(int count){
        try{
            int offset = BUFFSIZE - count;
            int ret = d_isIndex.read(d_buff,offset,count);
            if( ret < count && ret > 0){
                d_over = true;
                d_buffEnd = offset + ret;
            }
            d_ip = 0;
        }catch(Exception e){
            System.out.println(e.toString());
            return false;
        }
        return true;
    }
    int toInt(int i) {
        int t =((d_buff[i+0] & 0xff) <<24) | ((d_buff[i+1] & 0xff) <<16) | ((d_buff[i+2]  & 0xff) <<8) | (d_buff[i+3] & 0xff);
        return t;
    }
    int toInt(byte[] buff,int i){
        int t =((buff[i+0] & 0xff) <<24) | ((buff[i+1] & 0xff) <<16) | ((buff[i+2]  & 0xff) <<8) | (buff[i+3] & 0xff);
        return t;
    }
    boolean nextIndex(){
        if(d_ip >= d_buffEnd && d_over == true){
            return false;
        }
        int i = 0;
        do{

            for(i = d_ip; i < d_buffEnd; i++){
                if(d_buff[i] == 0x0)
                    break;
            } 
            if( (i + 9) > d_buffEnd ){
                if( d_over == false){
                    moveBuff();
                    if( !readIndexBuff(d_ip)){
                        return false;
                    }
                }else
                    return false;
            }else{
            //    if( d_ip == d_buffEnd)
             //       return false;
                byte[] w = new byte[i - d_ip];

                for(int j=d_ip; j<i; j++){
                    w[j-d_ip] = d_buff[j];
                } 
                d_word = new String(w);
                d_offset = d_buffBegin + d_ip;
                d_explainOffset = toInt(i+1);
                d_explainSize = toInt(i+5);
                d_ip = i + 9;
                d_count++;
            }
        }while (d_ip == 0);
        return true;
    }
    boolean preIndex(){
        int tmp = d_count--;
        init();
        d_count = --tmp;
        d_count = d_count <0 ? 0: d_count;
        try{
            d_isIndex.skip((int)d_offsetArray[d_count]);
            readIndexBuff(BUFFSIZE);
            nextIndex();
            d_count--;
        }catch(Exception e){
            return false;
        }
        return true;
    }
    void skipCache(String w){

        char first,second;
        if (w.length()>1){
            first = w.charAt(0);
            second = w.charAt(1);
        }else{
            first = w.charAt(0);
            second = 0x0;
        }
        try{
            long k;
            if (d_indexCache[first][second].d_flag == 1){
                k = d_isIndex.skip(d_indexCache[first][second].d_offset);
                d_count = d_indexCache[first][second].d_count;
            }
            else{
                k = d_isIndex.skip(d_indexCache[first][0].d_offset);
                d_count = d_indexCache[first][0].d_count;
            }
            readIndexBuff(BUFFSIZE);
        }catch(Exception e){
            System.out.println("error");
            System.out.println(e.toString());
        }
    }
    int min (int a, int b){
        return a > b ? b:a;
    }
    String findWord(String w){
        init();
        skipCache(w);
        int len = w.length();
        int max =0;
        int tempOffset =0;
        int tempSize = 0;
        String tempWord="";
        String W = w.toUpperCase();
        while(nextIndex())
        {
            int i;
            String Wd = d_word.toUpperCase();
            int maxLength = min(len,Wd.length());
            for (i =0 ;i < maxLength;i++){
                if (W.charAt(i) ==  Wd.charAt(i))
                    continue;
                else
                    break;
            }
            if (max <= i){
                if (max < i){
                tempOffset = d_explainOffset;
                tempSize = d_explainSize;
                max = i;
                if ( max == len)
                    break;
                }
            }
            else
                break;
        }
	return getWord(tempOffset,tempSize);
    }
    void dumpWordAndDict(){
	    System.out.println(d_word);
	    String w = getWord(d_explainOffset,d_explainSize);
	    System.out.println(w);
    }
    String findNextWord(){
	    nextIndex();
	    return getWord(d_explainOffset,d_explainSize);
    }
    String findPreWord(){
        preIndex();
	    return getWord(d_explainOffset,d_explainSize);
    }
    String getWord(int offset, int size){
	    try{
		    d_wordBuff = new byte[size];
		    d_isDict.close();
		    d_isDict = new FileInputStream(d_dictFileName + ".dict");
		    d_isDict.skip(offset);
		    d_isDict.read(d_wordBuff,0,size);
		    return new String(d_wordBuff);
	    }catch(Exception io){
		    return null;
	    }
    }
    void initCache(){
        d_indexCache = new Cache[CACHESIZE][CACHESIZE];
        for (int i = 0;i < CACHESIZE ;i++)
            for (int j=0; j < CACHESIZE; j++){
                d_indexCache[i][j] = new Cache();
                d_indexCache[i][j].d_flag = 0;
                d_indexCache[i][j].d_offset = 0;
            }
        try{
            int i,j;
            d_isCache = new FileInputStream(d_dictFileName + ".cache");
            d_isCache.read(d_buff,0,4);
            int w = toInt(0);
            byte [] buff = new byte[w * 16];
            d_isCache.read(buff,0,w * 16);
            for (int k = 0; k < w; k += 16){
                i = toInt(buff,k);
                j = toInt(buff,k + 4);
                d_indexCache[i][j].d_flag = 1 ;
                d_indexCache[i][j].d_offset = toInt(buff,k + 8);
                d_indexCache[i][j].d_count = toInt(buff,k + 16);
            }
            d_isCache.read(d_buff,0,4);
            int size = toInt(0);
            d_offsetArray = new int[size];
            buff = null;
            buff = new byte[size * 4];
            d_isCache.read(buff,0,size * 4);
            int count = 0;
            for ( i = 0;i < size;i++)
                d_offsetArray[i] = toInt(buff,i * 4);
            readIndexBuff(BUFFSIZE);
        }catch(Exception e){
            makeCache();
        }
    }
    String getCurrentWord(){
	    return d_word;
    }
    void msg(int a){
       System.out.print(a); 
       System.out.print(" ");
    }
    void msg(String a){
        System.out.print(a);
        System.out.print(" ");
    }
    void msgln(int a){
        System.out.println(a);
    }
    void msgln(String a){
        System.out.println(a);
    }
    void makeCache(){
            String w;
            char first,second;
            int count =0;
            int cacheCount = 0;
            ArrayList<Integer> list = new ArrayList<Integer>();
            list.add(0);
            readIndexBuff(BUFFSIZE);
        while(nextIndex()){
             w = d_word;
            if (w.length() > 1){
                first = w.charAt(0);
                second  = w.charAt(1);
            }else{
                first = w.charAt(0);
                second = 0x0;
            }

            if( first >= 0 && first < 128 && second >= 0 && second <= 128)
            {
            if (d_indexCache[first][second].d_flag == 0){
                d_indexCache[first][second].d_flag = 1;
                cacheCount++;
                d_indexCache[first][second].d_offset = d_offset;
                d_indexCache[first][second].d_count = count;
            }
            }
            list.add(d_offset);
            count++;
        }
        d_offsetArray = new int[list.size()];
        System.out.println(list.size());
        for (int i = 0; i < list.size(); i++){
            d_offsetArray[i] = list.get(i);
        }
        writeCache(cacheCount);
    }
    void writeCache(int cacheCount){
        int total;
        IntBuff  ib;
        int size = d_offsetArray.length;
        total = cacheCount * 4 * 4 + 4 + size * 4 + 4;
            
        ib = new IntBuff(total);
        int i=0 ;
        try{
            ib.storeInt(cacheCount);
            for(i=0 ; i < CACHESIZE;i++)
                for (int j = 0; j< CACHESIZE;j++){
                        if (d_indexCache[i][j].d_flag == 1){
                            ib.storeInt(i);
                            ib.storeInt(j);
                            ib.storeInt(d_indexCache[i][j].d_offset);
                            ib.storeInt(d_indexCache[i][j].d_count);
                        }
                    }
            ib.storeInt(size);
            for(i  = 0; i <  size;i++){
                 ib.storeInt(d_offsetArray[i]);
            }
            DataOutputStream os = new DataOutputStream(
                    new FileOutputStream(d_dictFileName + ".cache"));
            os.write(ib.getBuff(),0,ib.getByte());
            os.close();
        }catch(Exception e){
            msgln(e.toString());
            msgln(total);
            msgln(i);
            msgln(ib.getByte());
        }
    }
    public static void main(String [] args){
        Dict dict = new Dict("xiangya-medical");
	BufferedReader br = new BufferedReader(
			new InputStreamReader(System.in));
	while(true){
		try{
		String a = br.readLine();
		dict.findWord(a);
		dict.dumpWordAndDict();
		}catch(Exception e){
		}
	}
    }
}

