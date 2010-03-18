function file = find_mfile(classrootstr, subdirstr)

s = dir([classrootstr '*.mat']);    
file=[];
j = 1;

for i = 1:size(s,1)
    if( strcmp(subdirstr,'*') | strfind(s(i).name,subdirstr) )
	  f = strfind(classrootstr, '/');       
        %search for the delimeter /
        %if the last delimiter position found is the end of the string classrootstr
        %the classrootstr is a directory, so form file using it    
        if(f(size(f,2)) == size(classrootstr,2))
            file{j} = [classrootstr s(i).name];
        else %otherwise, trim off the end of the root and append the file name
            file{j} = [classrootstr(1:f(size(f,2))) s(i).name];
        end     
    end
end
