
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
% Script to tests collection for the user-interface code
%
% Author: Danelle Cline
% Date: Dec 2009
%

function test_classes_20060808T000000_4912_32_500()

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

dbroot='/Users/dcline'; 
%classalias='20060808T000000_4912_32_500-nophysical-object-noshadow-notrash-nomarine-organism+coryphaenoides'; 
%threshold=0.8; 
% test_class(dbroot, class1, classalias, threshold); 
% test_class(dbroot, class2, classalias, threshold);  
% test_class(dbroot, class3, classalias, threshold); 
% test_class(dbroot, class4, classalias, threshold); 
% test_class(dbroot, class5, classalias, threshold); 
% test_class(dbroot, class6, classalias, threshold); 
% test_class(dbroot, class7, classalias, threshold); 
% test_class(dbroot, class8, classalias, threshold); 
% test_class(dbroot, class9, classalias, threshold); 
% test_class(dbroot, class10, classalias, threshold); 
% test_class(dbroot, class11, classalias, threshold); 
% test_class(dbroot, class12, classalias, threshold); 
% test_class(dbroot, class13, classalias, threshold); 
% test_class(dbroot, class14, classalias, threshold); 
% test_class(dbroot, class15, classalias, threshold); 
% test_class(dbroot, class16, classalias, threshold); 
% test_class(dbroot, class17, classalias, threshold); 
% test_class(dbroot, class18, classalias, threshold); 
% test_class(dbroot, class20, classalias, threshold); 
% test_class(dbroot, class21, classalias, threshold); 
% test_class(dbroot, class22, classalias, threshold); 

% test_class(dbroot, class19, classalias, threshold); 
% 
% classnames=[{class1} {class2} {class3} {class20} {class21} {class22} {class5} {class6} {class7} {class8} {class9}  {class11} {class12} {class14}  {class15}  {class16} ];
% 
% dbroot='/Users/dcline'; 
classalias='20060808T000000_4912_32_500-final'; 
threshold=0.80;      
% test_class(dbroot, class2, classalias, threshold); 
% test_class(dbroot, class4, classalias, threshold);  
test_class(dbroot, class7, classalias, threshold); 
% test_class(dbroot, class9, classalias, threshold);     
% test_class(dbroot, class12, classalias, threshold);  
% test_class(dbroot, class15, classalias, threshold);   
end
