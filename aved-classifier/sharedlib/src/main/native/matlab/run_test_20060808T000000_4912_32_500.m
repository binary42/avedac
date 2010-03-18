%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
% Script to tests running the classifier against an entire directory
% for the user-interface code
%
% Author: Danelle Cline
% Date: Dec 2009
%

function run_test_20060808T000000_4912_32_500()

dbroot='/Users/dcline';
%color_space=2;

class1='Abyssocucumis abyssorum'
class2='Actiniaria'
class3='Amperima'
class4='Benthocodon'
class5='black frame'
class6='Coryphaenoides'
class7='Echinocrepis rostrata'
class8='gray frame'
class9='Hydroida'
class10='marine organism'
class11='Munidopsis'
class12='Mysida'
class13='not_evaluated'
class14='Oneirophanta mutabilis'
class15='Ophiuroidea'
class16='Peniagone'
class17='physical object'
class18='shadow'
class19='trash'
class20='Benthocodon_Round'
class21='Benthocodon_Pixalated'
class22='Benthocodon_Elongated'

%testdir='/var/tmp/20060808T000000_4912_32_500/20060808T000000_4912_32_500-testimages';
%collect_tests(testdir, dbroot, color_space);
testname='20060808T000000_4912_32_500-testimages'; 
%trainingalias='20060808T000000_4912_32_500-nophysical-object';
trainingalias='20060808T000000_4912_32_500';
%trainingalias='20060808T000000_4912_32_500-nophysical-object-noshadow-notrash-nomarine-organism+coryphaenoides';
%trainingalias=[ '20060808T000000_4912_32_500' class20 class21 class22] ;
%trainingalias='20060808T000000_4912_32_500-final';
threshold=0.5 ;   
run_tests_ui(dbroot, testname, trainingalias, threshold)

end
