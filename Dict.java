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
    FileInputStream d_isIndex = null;
    FileInputStream d_isDict = null;
    FileInputStream d_isCache = null;
    


    int [] d_offsetArray;
    Cache[][] d_indexCache;
    String d_word;
    boolean d_over;
    byte [] d_buff;
    byte [] d_wordBuff;
    int d_buffBegin;
    int d_buffEnd;
    int d_ip;
    int d_offset;
    int d_wordOffset;
    int d_wordSize;
    int d_count;
    final int BUFFSIZE = 40960;
    final int CACHESIZE = 128;
    String d_dictFileName=null;

    Dict(String filename) 
    {
        d_dictFileName = filename;
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
    boolean readBuff(FileInputStream is, boolean needMove){
        if( needMove){
            for(int i = d_ip,j=0; i < d_buffEnd; i++,j++){
                d_buff[j] = d_buff[i];
            } 
            d_buffBegin += d_ip;
        }else{
            d_ip = BUFFSIZE;
        }
        try{
            int ret = is.read(d_buff,d_buffEnd - d_ip,d_ip);
            if( ret < d_ip && ret > 0){
                d_over = true;
                d_buffEnd = d_buffEnd - d_ip + ret;
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
                    if( !readBuff(d_isIndex,true)){
                        return false;
                    }
                }else
                    return false;
            }else{
                if( d_ip == d_buffEnd)
                    return false;
                byte[] w = new byte[i - d_ip];

                for(int j=d_ip; j<i; j++){
                    w[j-d_ip] = d_buff[j];
                } 
                d_word = new String(w);
                d_offset = d_buffBegin + d_ip;
                d_wordOffset = toInt(i+1);
                d_wordSize = toInt(i+5);
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
        readBuff(d_isIndex,false);
        nextIndex();
        d_count--;
        }catch(Exception e){
            return false;
        }
        return true;
    }
    void run(){
        if (!init())
            return;
        readBuff(d_isIndex,false);
        initCache();
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
            readBuff(d_isIndex,false);
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
                tempOffset = d_wordOffset;
                tempSize = d_wordSize;
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
	    String w = getWord(d_wordOffset,d_wordSize);
	    System.out.println(w);
    }
    String findNextWord(){
	    nextIndex();
	    return getWord(d_wordOffset,d_wordSize);
    }
    String findPreWord(){
        preIndex();
	    return getWord(d_wordOffset,d_wordSize);
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
            d_isCache = new FileInputStream(d_dictFileName + ".cache");
            int w;
            for (int i = 0;i < CACHESIZE ;i++)
                for (int j=0; j < CACHESIZE; j++){
                    d_isCache.read(d_buff,0,12);
                    d_indexCache[i][j].d_flag = toInt(0) ;
                    d_indexCache[i][j].d_offset = toInt(4);
                    d_indexCache[i][j].d_count = toInt(8);
                }
            d_isCache.read(d_buff,0,4);
            d_offsetArray = new int[toInt(0)];
            int count = 0;
            while(d_isCache.read(d_buff,0,4) != -1){
                d_offsetArray[count++] = toInt(0);
            }
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
            ArrayList<Integer> list = new ArrayList<Integer>();
            list.add(0);
        while(nextIndex()){
             w = d_word;
            if (w.length() > 1){
                first = w.charAt(0);
                second  = w.charAt(1);
            }else{
                first = w.charAt(0);
                second = 0x0;
            }
        //    System.out.print(first);
         //   System.out.print(" ");
          //  System.out.println(second);
          //msg (d_word); 
          //msg (d_wordOffset);
          //msgln (d_wordSize);

            if( first >= 0 && first < 128 && second >= 0 && second <= 128)
            {
            if (d_indexCache[first][second].d_flag == 0){
                d_indexCache[first][second].d_offset = d_offset;
                d_indexCache[first][second].d_flag = 1;
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
        writeCache();
    }
    void writeCache(){
        try{
            DataOutputStream os = new DataOutputStream(
                    new FileOutputStream(d_dictFileName + ".cache"));
            for(int i=0 ; i < CACHESIZE;i++)
                for (int j = 0; j< CACHESIZE;j++){
                        os.writeInt(d_indexCache[i][j].d_flag);
                        os.writeInt(d_indexCache[i][j].d_offset);
                        os.writeInt(d_indexCache[i][j].d_count);
                    }
            os.writeInt((int)d_offsetArray.length);
            for(int  i  = 0; i <  d_offsetArray.length;i++)
               os.writeInt((int)d_offsetArray[i]);
            os.close();
        }catch(Exception e){
        }
    }
    public static void main(String [] args){
        Dict dict = new Dict("xiangya-medical");
        dict.run();
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

