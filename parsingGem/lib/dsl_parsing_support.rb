require 'open4'
require 'json'

class DslParsingSupport

  JAR_PATH = File.expand_path('../../bin/genXlsParsingWithProvenance.jar', __FILE__)
  TEST_PATH = '/home/bittkomk/randomProjects/genXlsParsingWithProvenance'

  def self.test_parse_xls_to_json
     parse_xls_to_json("#{TEST_PATH}/bode-surgical.parser",
                       "#{TEST_PATH}/SEEK-Tierliste_MG_complete.xlsx",
                       "#{TEST_PATH}/gem-parsed-bode-2.json") 
  end

  def self.parse_xls_to_json parser_def, xls_input_file, json_output_file
    data_hash = nil    
    if (invoke_cmd create_cmd(parser_def, xls_input_file, json_output_file)) == 0
      data_hash = JSON.parse(File.read(json_output_file))
    end
    data_hash
  end

  private
  def self.create_cmd parser_def, xls_input_file, json_output_file, xls_output_file=nil, embed_provenance_information=nil
    cmd = "java -jar #{JAR_PATH} #{parser_def} -inputFile #{xls_input_file}"
    cmd = cmd + " -jsonOutputFile #{json_output_file}" if json_output_file
    cmd = cmd + " -outputFile #{xls_output_file}" if xls_output_file
    cmd = cmd + " -embedProvencanceInformation" if embed_provenance_information
    cmd
  end

  def self.invoke_cmd cmd
   # cmd = "java -jar #{JAR_PATH} #{TEST_PATH}/bode-surgical.parser -inputFile #{TEST_PATH}/SEEK-Tierliste_MG_complete.xlsx -jsonOutputFile #{TEST_PATH}/rubyGem-output.json" 
    
    puts "invoke #{cmd}"

    output = ""

    status = Open4::popen4(cmd) do |pid, stdin, stdout, stderr|
      while((line = stdout.gets) != nil) do
        output << line
      end
      stdout.close
      stderr.close
    end

     puts output.strip
     status.to_i
  end

end
