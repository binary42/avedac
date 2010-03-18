function state = isKill(file)
%fprintf(1,'Checking kill file %s \n',file);
state =0;
fid = fopen(file,'rb');
if(fid > 0)
    a=fread(fid, 1,'uint8');
    fclose(fid);
    if(a ~= 0)
        state = 1;
    end
    
end