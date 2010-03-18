%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
% Script to tests collection for the user-interface code
%
% Author: Danelle Cline
% Date: Dec 2009
% 

function train_classes_stationm()

dbroot='/Users/dcline';   
classalias='20060808T000000_4912_32_500';

class1='Abyssocucumis abyssorum'
classalias=[ '20060808T000000_4912_32_500' class1] ;
classnames=[{class1} ];
%train_classes_ui(dbroot, classalias, classnames, classalias); 

class2='Actiniaria'
classalias=[ '20060808T000000_4912_32_500' class2] ;
classnames=[{class2} ];
train_classes_ui(dbroot, classalias, classnames, classalias); 

class3='Amperima'
classalias=[ '20060808T000000_4912_32_500' class3] ;
classnames=[{class3} ];
%train_classes_ui(dbroot, classalias, classnames, classalias); 

class4='Benthocodon'
classalias=[ '20060808T000000_4912_32_500' class4] ;
classnames=[{class4} ];
train_classes_ui(dbroot, classalias, classnames, classalias);  

class5='black frame'
classalias=[ '20060808T000000_4912_32_500' class5] ;
classnames=[{class5} ];
%train_classes_ui(dbroot, classalias, classnames, classalias);  

class6='Coryphaenoides'
classalias=[ '20060808T000000_4912_32_500' class5] ;
classnames=[{class5} ];
%train_classes_ui(dbroot, classalias, classnames, classalias);  

class7='Echinocrepis rostrata'
classalias=[ '20060808T000000_4912_32_500' class7] ;
classnames=[{class7} ];
train_classes_ui(dbroot, classalias, classnames, classalias);  

class8='gray frame'
classalias=[ '20060808T000000_4912_32_500' class8] ;
classnames=[{class8} ];
%train_classes_ui(dbroot, classalias, classnames, classalias);  

class9='Hydroida'
classalias=[ '20060808T000000_4912_32_500' class9] ;
classnames=[{class9} ];
train_classes_ui(dbroot, classalias, classnames, classalias);  

class10='marine organism'
classalias=[ '20060808T000000_4912_32_500' class10] ;
classnames=[{class10} ];
train_classes_ui(dbroot, classalias, classnames, classalias);  

class11='Munidopsis'
classalias=[ '20060808T000000_4912_32_500' class11] ;
classnames=[{class11} ];
%train_classes_ui(dbroot, classalias, classnames, classalias);  

class12='Mysida'
classalias=[ '20060808T000000_4912_32_500' class12] ;
classnames=[{class12} ];
train_classes_ui(dbroot, classalias, classnames, classalias);  

class13='not_evaluated'

class14='Oneirophanta mutabilis'
classalias=[ '20060808T000000_4912_32_500' class14] ;
classnames=[{class14} ];
%train_classes_ui(dbroot, classalias, classnames, classalias);  

class15='Ophiuroidea'
classalias=[ '20060808T000000_4912_32_500' class15] ;
classnames=[{class15} ];
train_classes_ui(dbroot, classalias, classnames, classalias);  

class16='Peniagone'
classalias=[ '20060808T000000_4912_32_500' class16] ;
classnames=[{class16} ];
%train_classes_ui(dbroot, classalias, classnames, classalias);  

class17='physical object'
classalias=[ '20060808T000000_4912_32_500' class17] ;
classnames=[{class17} ];
%train_classes_ui(dbroot, classalias, classnames, classalias);  

class18='shadow'
classalias=[ '20060808T000000_4912_32_500' class18] ;
classnames=[{class18} ];
%train_classes_ui(dbroot, classalias, classnames, classalias);  

class19='trash'
classalias=[ '20060808T000000_4912_32_500' class19] ;
classnames=[{class19} ];
%train_classes_ui(dbroot, classalias, classnames, classalias);  

class20='Benthocodon_Round'
class21='Benthocodon_Pixalated'
class22='Benthocodon_Elongated'
classalias=[ '20060808T000000_4912_32_500' class20 class21 class22] ;
classnames=[{class20} {class21} {class22} ];
train_classes_ui(dbroot, classalias, classnames, classalias);  

classnames=[{class1} {class2} {class3} {class4} {class5} {class7} {class8} {class9}  {class10} {class11} {class12} {class14}  {class15}  {class16}  {class17}  {class18}  {class19}];
%train_classes_ui(dbroot, classalias, classnames, classalias);  


classalias='20060808T000000_4912_32_500-nophysical-object'; 
classnames=[{class1} {class2} {class3} {class4} {class5} {class7} {class8} {class9}  {class10} {class11} {class12} {class14}  {class15}  {class16}  {class18}  {class19}];
%train_classes_ui(dbroot, classalias, classnames, classalias);  

classalias='20060808T000000_4912_32_500-final';
classnames=[ {class2} {class4} {class7} {class9} {class12} {class15} ];
train_classes_ui(dbroot, classalias, classnames, classalias);  
end
