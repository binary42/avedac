%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
% Test event-wise classification by majority win or
% a probability rule that assigns a class winner 
% with the index of highest probability in at
% least 30% of the total frames   
%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

function [recfiles, event_classifier_results] = event_classifier(method, classes, classindex, probabilityindex, filenames)

switch lower(method)

   case {'majority','major'}

      disp('Running event classifier method majority wins')      
      event_classifier_results(1,:) = {'EVENT CLASSIFIER METHOD:',method,'','',''};   
      event_classifier_results(2,:) = {'EVENTID','CLASS NAME', 'CLASS INDEX','TTL FRAMES', 'FRAMES FOUND IN CLASS'};   

   case {'probability', 'prob'}

      disp('Running event classifier method probability wins')      
      event_classifier_results(1,:) = {'EVENT CLASSIFIER METHOD:',method,'','','',''};   
      event_classifier_results(2,:) = {'EVENTID','CLASS NAME', 'CLASS INDEX','TTL FRAMES', 'FRAMES FOUND IN CLASS', 'PROBABILITY IN CLASS'};   

   otherwise

      disp('Unknown method.')
      return;
      
end    

% name of the classes from training and test set and new matrix to store
% file names
recfiles(1,:) = [classes];

%get the next filename
str = filenames{2};

%find ending index of event identifier _evt
m = regexp(str, '\.*?evt[0-9]+\.*?','match');

%get event id string - this assumes only one match
id = m{:};      

%event identifier string
eventid = id;
eventstr = {str};

%start and end frame indices
efi=1;
sfi=1;
ii=1;
maxlen=length(filenames);

%iterate through all files
while  ii < maxlen           

    %search for the end of this event
    %by looking until the event id changes
    while  strcmp(id, eventid)                 
        
        %increment the ending frame index
        efi = ii;         

        %increment the index                     
        ii = ii + 1;                       

        %check to make sure don't access beyond the last filename
        if ii < maxlen           

            %get the next filename
            str = filenames{ii + 1};    

            %find ending index of event identifier evt
            m = regexp(str, '\.*?evt[0-9]+\.*?','match'); 

            %get event id string - this assumes only one unique match
            id = m{:};         
            
        else

            break;

        end

    end    

    %now calculate the winner
    %calculate the total frames
    ttlframes = efi-sfi+1;        

    %calculate the winner based on the chosen method
    switch lower(method)

        case {'majority','major'}

            %initialize class sum matrix
            classsum = zeros(1,length(classes));           

            for jj = 1:length(classes)

                %sum the total the classes of this event
                classsum(jj) = length(find(classindex(sfi:efi) == jj));

            end                    

            %find the index of the majority
            winner = find(classsum == max(classsum));                        

            %if there is a tie, set to UNK category
            if length(winner) > 1

                winner = 1;

            end
            
            % store the resuls in a matrix
            recfiles(end+1, winner) = eventstr;

            %store the results in the results table            
            event_classifier_results(end+1,:)={eventid,classes{winner},winner,ttlframes,classsum(winner)};         

        case {'probability', 'prob'}            

            %initialize class sum and probability matrix
            classsum = zeros(1,length(classes));

            probability = zeros(1,length(classes));            

            for jj = 1:length(classes)

                %find all class indexes that match this class
                i = find(classindex(sfi:efi) == jj);

                %sum the total classes
                classsum(jj) = length(i);

                %find the total averaged probability for this class
                probability(jj) = sum(probabilityindex(i))/max(1,classsum(jj));                

            end

            %find the index of highest probability winner with at
            %least 30% of the total frames
            a = find(classsum/ttlframes >= 0.30);
            b = find(probability(a) == max(probability(a)));

            %if a tie for the maximum probability, or all probabilities are
            %the same, or all probabilities are less than 30% of the total frames,
            %then break default to UNK
            if(length(b) > 1 | isempty(b) | isempty(a))
                winner = 1;
            else
                winner = find(probability == probability(a(b)));
            end            

            %if there is a tie, break the tie with that with the
            %higher total frames
            if length(winner) > 1
                winner = find( classsum(winner) == max(classsum(winner)));
            end            

            %this is arbitrary, but if there is still a tie, just
            %choose the first one
            if length(winner) > 1
                winner = winner(1);
            end

            %if no winner, set to UNK category
            if isempty(winner)
                winner = 1;
            end            
            
            % store the resuls in a matrix
            recfiles(end+1, winner) = eventstr;

            %store the results in the results table
            event_classifier_results(end+1,:)={eventid,classes{winner},winner,ttlframes,classsum(winner), probability(winner)};    

    end     

    %reset the start index and event id
    eventid = id;    
    eventstr = {str};

    %reset the start and end indices
    sfi = efi+1;            
    efi = sfi;    

end %end iterate through all the files

end % end function

