$TESTING=true
JRUBY = RUBY_PLATFORM =~ /java/

require 'rubygems'

gem 'rspec', '>1.1.12'
require 'spec'

require 'date'
require 'ostruct'
require 'pathname'
require 'fileutils'

# put data_objects from repository in the load path
# DO NOT USE installed gem of data_objects!
$:.unshift File.expand_path(File.join(File.dirname(__FILE__), '..', '..', 'data_objects', 'lib'))
require 'data_objects'

DATAOBJECTS_SPEC_ROOT = Pathname(__FILE__).dirname.parent.parent + 'data_objects' + 'spec'
Pathname.glob((DATAOBJECTS_SPEC_ROOT + 'lib/**/*.rb').to_s).each { |f| require f }

if JRUBY
  $:.unshift File.expand_path(File.join(File.dirname(__FILE__), '..', '..', 'do_jdbc', 'lib'))
  require 'do_jdbc'
end

# put the pre-compiled extension in the path to be found
$:.unshift File.expand_path(File.join(File.dirname(__FILE__), '..', 'lib'))
require 'do_postgres'

log_path = File.expand_path(File.join(File.dirname(__FILE__), '..', 'log', 'do.log'))
FileUtils.mkdir_p(File.dirname(log_path))

DataObjects::Postgres.logger = DataObjects::Logger.new(log_path, :debug)

at_exit { DataObjects.logger.flush }

Spec::Runner.configure do |config|
  config.include(DataObjects::Spec::PendingHelpers)
end

CONFIG = OpenStruct.new
CONFIG.scheme   = 'postgres'
CONFIG.user     = ENV['DO_POSTGRES_USER'] || 'postgres'
CONFIG.pass     = ENV['DO_POSTGRES_PASS'] || ''
CONFIG.host     = ENV['DO_POSTGRES_HOST'] || 'localhost'
CONFIG.port     = ENV['DO_POSTGRES_PORT'] || '5432'
CONFIG.database = ENV['DO_POSTGRES_DATABASE'] || '/do_test'

CONFIG.uri = ENV["DO_POSTGRES_SPEC_URI"] ||"#{CONFIG.scheme}://#{CONFIG.user}:#{CONFIG.pass}@#{CONFIG.host}:#{CONFIG.port}#{CONFIG.database}"
CONFIG.sleep = "SELECT pg_sleep(1)"

module DataObjectsSpecHelpers

  def setup_test_environment
    conn = DataObjects::Connection.new(CONFIG.uri)

    conn.create_command(<<-EOF).execute_non_query
      DROP TABLE IF EXISTS "invoices"
    EOF

    conn.create_command(<<-EOF).execute_non_query
      DROP TABLE IF EXISTS "users"
    EOF

    conn.create_command(<<-EOF).execute_non_query
      DROP TABLE IF EXISTS "widgets"
    EOF

    conn.create_command(<<-EOF).execute_non_query
      CREATE TABLE "users" (
        "id" SERIAL,
        "name" VARCHAR(200) default 'Billy' NULL,
        "fired_at" timestamp,
        PRIMARY KEY  ("id")
      );
    EOF

    conn.create_command(<<-EOF).execute_non_query
      CREATE TABLE "invoices" (
        "id" SERIAL,
        "invoice_number" varchar(50) NOT NULL,
        PRIMARY KEY  ("id")
      );
    EOF

    conn.create_command(<<-EOF).execute_non_query
      CREATE TABLE "widgets" (
        "id" SERIAL,
        "code" char(8) default 'A14' NULL,
        "name" varchar(200) default 'Super Widget' NULL,
        "shelf_location" text NULL,
        "description" text NULL,
        "image_data" bytea NULL,
        "ad_description" text NULL,
        "ad_image" bytea NULL,
        "whitepaper_text" text NULL,
        "cad_drawing" bytea NULL,
        "flags" boolean default false,
        "number_in_stock" smallint default 500,
        "number_sold" integer default 0,
        "super_number" bigint default 9223372036854775807,
        "weight" float default 1.23,
        "cost1" double precision default 10.23,
        "cost2" decimal(8,2) default 50.23,
        "release_date" date default '2008-02-14',
        "release_datetime" timestamp default '2008-02-14 00:31:12',
        "release_timestamp" timestamp with time zone default '2008-02-14 00:31:31',
        PRIMARY KEY  ("id")
      );
    EOF

    1.upto(16) do |n|
      conn.create_command(<<-EOF).execute_non_query
        insert into widgets(code, name, shelf_location, description, image_data, ad_description, ad_image, whitepaper_text, cad_drawing, super_number, weight) VALUES ('W#{n.to_s.rjust(7,"0")}', 'Widget #{n}', 'A14', 'This is a description', 'IMAGE DATA', 'Buy this product now!', 'AD IMAGE DATA', 'String', 'CAD \\001 \\000 DRAWING'::bytea, 1234, 13.4);
      EOF
    end

    conn.create_command(<<-EOF).execute_non_query
      update widgets set flags = true where id = 2
    EOF

    conn.create_command(<<-EOF).execute_non_query
      update widgets set ad_description = NULL where id = 3
    EOF

    conn.close

  end

end
