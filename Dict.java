import java.io.FileInputStream;
class Dict{
    class Cache{
        boolean d_flag;
        int d_offset;
    };
    FileInputStream d_isIndex;
    FileInputStream d_isDict;
    FileInputStream d_isCache;
    

    int d_buffBegin;
    int d_buffEnd;
    byte [] d_buff;
    byte [] d_wordBuff;
    int d_ip;
    boolean d_over;
    int d_offset;
    int d_wordOffset;
    int d_wordSize;
    String d_word;
    Cache[][] d_indexCache;

    final int BUFFSIZE = 40960;
    final int CACHESIZE = 128;
    boolean init(){
        try{
            d_isIndex = new FileInputStream("longman.idx");
            d_isDict  = new FileInputStream("longman.dict");
            //d_isCache = new FileInputStream("longman.cache");
            d_buffBegin = 0;
            d_buffEnd = BUFFSIZE;
            d_buff = new byte[BUFFSIZE];
            d_ip = 0;
            d_over = false;
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
            }
        }while (d_ip == 0);
        return true;
    }
    void run(){
        if (!init())
            return;
        readBuff(d_isIndex,false);
        initCache();
        makeCache();
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
            if (d_indexCache[first][second].d_flag == true)
                k = d_isIndex.skip(d_indexCache[first][second].d_offset);
            else
                k = d_isIndex.skip(d_indexCache[first][0].d_offset);
            readBuff(d_isIndex,false);
        }catch(Exception e){
            System.out.println("error");
            System.out.println(e.toString());
        }
    }
    int min (int a, int b){
        return a > b ? b:a;
    }
    void findWord(String w){
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
                tempWord = d_word;
                max = i;
                if ( max == len)
                    //break;
                    ;
                }
            }
            else
                break;
        }
        System.out.println(w);
        System.out.println(tempWord);
        d_wordBuff = new byte[tempSize];
        try{
            d_isDict.skip(tempOffset);
            d_isDict.read(d_wordBuff,0,tempSize);
            System.out.println(new String(d_wordBuff));
        }catch(Exception e){
            System.out.println(e.toString());
        }
    }
    void initCache(){
    d_indexCache = new Cache[CACHESIZE][CACHESIZE];
        for (int i = 0;i < CACHESIZE ;i++)
            for (int j=0; j < CACHESIZE; j++){
                d_indexCache[i][j] = new Cache();
                d_indexCache[i][j].d_flag = false;
                d_indexCache[i][j].d_offset = 0;
            }
    }
    void makeCache(){
            String w;
            char first,second;
        while(nextIndex()){
             w = d_word;
            if (w.length() > 1){
                first = w.charAt(0);
                second  = w.charAt(1);
            }else{
                first = w.charAt(0);
                second = 0x0;
            }
            if (d_indexCache[first][second].d_flag == false){
                d_indexCache[first][second].d_offset = d_offset;
                d_indexCache[first][second].d_flag = true;
            }
        }
    }
    public static void main(String [] args){
        Dict dict = new Dict();
        dict.run();
        dict.findWord(args[0]);
    }
}

