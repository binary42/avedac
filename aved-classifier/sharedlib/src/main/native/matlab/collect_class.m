%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
% Script to collect data files to be used for training actual classes
%
% Author: Marc'Aurelio Ranzato
% Date: Dec 2003
%
%Modified by doliver@mbari.org on December 5, 2004

function classfilemetadata = collect_class(kill, dbroot, classdir)

%check class directory
if(isdir(classdir) == 0)
   error('Error - %s is not a valid directory\n', classdir);
   return
end

%set classname to root directory name
classname = find_root_dir(classdir);

%collect classes
fprintf(1, 'Collecting training file data\n');
[classfilemetadata, classdata] = collect(kill, dbroot,classdir,classname);

end
