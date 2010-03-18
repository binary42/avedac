%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
% Script to tests collection for the user-interface code
%
% Author: Danelle Cline
% Date: Dec 2009
% 

function collect_stationm()


dbroot='/Users/dcline';
color_space=2; %RGB
%class='Abyssocucumis abyssorum'
%dirct_or_filelist='/Volumes/nanomiaRAID-1/AVED_StaM_900905/Images-AVED-processed/20060808T000000_4912_32_500/TrainingLibrariesSquared/Abyssocucumis abyssorum'; 
%collect_ui(dirct_or_filelist, dirct_or_filelist, class, dbroot, color_space, class, class);
% class='Actiniaria'
% dirct_or_filelist='/Volumes/nanomiaRAID-1/AVED_StaM_900905/Images-AVED-processed/20060808T000000_4912_32_500/TrainingLibrariesSquared/Actiniaria';
% collect_ui(dirct_or_filelist, dirct_or_filelist, class, dbroot, color_space, class, class);
% class='Amperima'
% dirct_or_filelist='/Volumes/nanomiaRAID-1/AVED_StaM_900905/Images-AVED-processed/20060808T000000_4912_32_500/TrainingLibrariesSquared/Amperima';
% collect_ui(dirct_or_filelist, dirct_or_filelist, class, dbroot, color_space, class, class);
% class='Benthocodon'
% dirct_or_filelist='/Volumes/nanomiaRAID-1/AVED_StaM_900905/Images-AVED-processed/20060808T000000_4912_32_500/TrainingLibrariesSquared/Benthocodon';
% collect_ui(dirct_or_filelist, dirct_or_filelist, class, dbroot, color_space, class, class);
% class='Benthocodon_Round'
% dirct_or_filelist='/Volumes/nanomiaRAID-1/AVED_StaM_900905/Images-AVED-processed/20060808T000000_4912_32_500/TrainingLibrariesSquared/Benthocodon_Round';
% collect_ui(dirct_or_filelist, dirct_or_filelist, class, dbroot, color_space, class, class);
% class='Benthocodon_Pixalated'
% dirct_or_filelist='/Volumes/nanomiaRAID-1/AVED_StaM_900905/Images-AVED-processed/20060808T000000_4912_32_500/TrainingLibrariesSquared/Benthocodon_Pixalated';
% collect_ui(dirct_or_filelist, dirct_or_filelist, class, dbroot, color_space, class, class);
% class='Benthocodon_Elongated'
% dirct_or_filelist='/Volumes/nanomiaRAID-1/AVED_StaM_900905/Images-AVED-processed/20060808T000000_4912_32_500/TrainingLibrariesSquared/Benthocodon_Elongated';
% collect_ui(dirct_or_filelist, dirct_or_filelist, class, dbroot, color_space, class, class);
% class='black frame' 
% dirct_or_filelist='/Volumes/nanomiaRAID-1/AVED_StaM_900905/Images-AVED-processed/20060808T000000_4912_32_500/TrainingLibrariesSquared/black frame';
% collect_ui(dirct_or_filelist, dirct_or_filelist, class, dbroot, color_space, class, class);
% class='Coryphaenoides'
% dirct_or_filelist='/Volumes/nanomiaRAID-1/AVED_StaM_900905/Images-AVED-processed/20060808T000000_4912_32_500/TrainingLibrariesSquared/Coryphaenoides';
% collect_ui(dirct_or_filelist, dirct_or_filelist, class, dbroot, color_space, class, class);
% class='Echinocrepis rostrata' 
% dirct_or_filelist='/Volumes/nanomiaRAID-1/AVED_StaM_900905/Images-AVED-processed/20060808T000000_4912_32_500/TrainingLibrariesSquared/Echinocrepis rostrata';
% collect_ui(dirct_or_filelist, dirct_or_filelist, class, dbroot, color_space, class, class);
% class='gray frame'
% dirct_or_filelist='/Volumes/nanomiaRAID-1/AVED_StaM_900905/Images-AVED-processed/20060808T000000_4912_32_500/TrainingLibrariesSquared/gray frame';
% collect_ui(dirct_or_filelist, dirct_or_filelist, class, dbroot, color_space, class, class);
% class='Hydroida'
% dirct_or_filelist='/Volumes/nanomiaRAID-1/AVED_StaM_900905/Images-AVED-processed/20060808T000000_4912_32_500/TrainingLibrariesSquared/Hydroida';
% collect_ui(dirct_or_filelist, dirct_or_filelist, class, dbroot, color_space, class, class);
% class='marine organism'
% dirct_or_filelist='/Volumes/nanomiaRAID-1/AVED_StaM_900905/Images-AVED-processed/20060808T000000_4912_32_500/TrainingLibrariesSquared/marine organism';
% collect_ui(dirct_or_filelist, dirct_or_filelist, class, dbroot, color_space, class, class);
% class='Munidopsis'
% dirct_or_filelist='/Volumes/nanomiaRAID-1/AVED_StaM_900905/Images-AVED-processed/20060808T000000_4912_32_500/TrainingLibrariesSquared/Munidopsis';
% collect_ui(dirct_or_filelist, dirct_or_filelist, class, dbroot, color_space, class, class);
% class='Mysida'
% dirct_or_filelist='/Volumes/nanomiaRAID-1/AVED_StaM_900905/Images-AVED-processed/20060808T000000_4912_32_500/TrainingLibrariesSquared/Mysida';
% collect_ui(dirct_or_filelist, dirct_or_filelist, class, dbroot, color_space, class, class);
% class='not_evaluated'
% dirct_or_filelist='/Volumes/nanomiaRAID-1/AVED_StaM_900905/Images-AVED-processed/20060808T000000_4912_32_500/TrainingLibrariesSquared/not_evaluated';
% collect_ui(dirct_or_filelist, dirct_or_filelist, class, dbroot, color_space, class, class);
class='Oneirophanta mutabilis'
dirct_or_filelist='/Volumes/nanomiaRAID-1/AVED_StaM_900905/Images-AVED-processed/20060808T000000_4912_32_500/TrainingLibrariesSquared/Oneirophanta mutabilis';
collect_ui(dirct_or_filelist, dirct_or_filelist, class, dbroot, color_space, class, class);
class='Ophiuroidea'
dirct_or_filelist='/Volumes/nanomiaRAID-1/AVED_StaM_900905/Images-AVED-processed/20060808T000000_4912_32_500/TrainingLibrariesSquared/Ophiuroidea';
collect_ui(dirct_or_filelist, dirct_or_filelist, class, dbroot, color_space, class, class);
class='Peniagone'
dirct_or_filelist='/Volumes/nanomiaRAID-1/AVED_StaM_900905/Images-AVED-processed/20060808T000000_4912_32_500/TrainingLibrariesSquared/Peniagone';
collect_ui(dirct_or_filelist, dirct_or_filelist, class, dbroot, color_space, class, class);
class='physical object'
dirct_or_filelist='/Volumes/nanomiaRAID-1/AVED_StaM_900905/Images-AVED-processed/20060808T000000_4912_32_500/TrainingLibrariesSquared/physical object';
collect_ui(dirct_or_filelist, dirct_or_filelist, class, dbroot, color_space, class, class);
class='shadow'
dirct_or_filelist='/Volumes/nanomiaRAID-1/AVED_StaM_900905/Images-AVED-processed/20060808T000000_4912_32_500/TrainingLibrariesSquared/shadow';
collect_ui(dirct_or_filelist, dirct_or_filelist, class, dbroot, color_space, class, class);
class='trash'
dirct_or_filelist='/Volumes/nanomiaRAID-1/AVED_StaM_900905/Images-AVED-processed/20060808T000000_4912_32_500/TrainingLibrariesSquared/trash';
collect_ui(dirct_or_filelist, dirct_or_filelist, class, dbroot, color_space, class, class);

end
