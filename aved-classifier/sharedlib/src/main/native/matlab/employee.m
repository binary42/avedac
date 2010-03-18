classdef employee < handle 
   properties
      Name = ''
      Department = '';
   end
   methods
      function e = employee(name,dept)
         e.Name = name;
         e.Department = dept;
      end % employee
      function transfer(obj,newDepartment)
         obj.Department = newDepartment;
      end % transfer
   end
end